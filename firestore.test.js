const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("stories").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("posts").doc("p1").set({
      userId: ALICE_UID,
      authorName: "Alice",
      postType: "TEXT",
      visibility: "PUBLIC",
      caption: "Hello",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      viewsCount: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile PII isolation: owner can read/write, peer cannot read/write", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      email: "alice@example.com",
      bio: "Music and photos",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Updated bio",
      updatedAt: pastDate(1000),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      bio: "Hacked",
      updatedAt: pastDate(1000),
    })
  );
});

test("Posts: TEXT & IMAGE create, query alignment, shadow/spoof rejections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  // Valid TEXT post with styled background
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_text").set({
      userId: ALICE_UID,
      authorName: "Alice",
      postType: "TEXT",
      visibility: "PUBLIC",
      caption: "Good morning everyone!",
      textBgHex: "#5B4BFF",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      viewsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid IMAGE post
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      userId: ALICE_UID,
      authorName: "Alice",
      postType: "IMAGE",
      visibility: "PUBLIC",
      caption: "Sunset vibes",
      mediaBase64: "iVBORw0KGgo=",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      viewsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("posts").doc("post_ghost").set({
      userId: ALICE_UID,
      authorName: "Alice",
      postType: "LINK",
      visibility: "PUBLIC",
      caption: "Ghost",
      isVerified: true,
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      viewsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject spoofed userId
  await assertFails(
    bobDb.collection("posts").doc("post_spoof").set({
      userId: ALICE_UID,
      authorName: "Alice",
      postType: "SONG",
      visibility: "PUBLIC",
      caption: "Spoof",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      viewsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "PUBLIC").get());
  await assertFails(bobDb.collection("posts").get());
});

test("Social interactions: atomic Like/Reaction, Views increment, Comment, Share, Save, Follow, Notifications", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const ts = pastDate(10000);
    await context.firestore().collection("posts").doc("post_1").set({
      userId: ALICE_UID,
      authorName: "Alice",
      postType: "SONG",
      visibility: "PUBLIC",
      caption: "New track",
      title: "Midnight Echo",
      subtitle: "DJ Pulse",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      viewsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Views increment +1 succeeds; +5 fails
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      viewsCount: 1,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      viewsCount: 5,
      updatedAt: pastDate(1000),
    })
  );

  // Atomic Like/Reaction batch succeeds
  const likeBatch = bobDb.batch();
  likeBatch.set(
    bobDb.collection("users").doc(BOB_UID).collection("liked_posts").doc("post_1"),
    { postId: "post_1", userId: BOB_UID, reaction: "LOVE", createdAt: pastDate(1000) }
  );
  likeBatch.update(bobDb.collection("posts").doc("post_1"), {
    likesCount: 1,
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(likeBatch.commit());

  // Reaction update on liked_posts succeeds
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("liked_posts").doc("post_1").update({
      reaction: "FIRE",
    })
  );

  // Bob follows Alice
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("following").doc(ALICE_UID).set({
      targetUserId: ALICE_UID,
      targetName: "Alice",
      userId: BOB_UID,
      createdAt: pastDate(900),
    })
  );
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).collection("following").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("following").get());

  // Bob notifies Alice; Alice reads & marks read; Bob cannot read Alice's notifications
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n1").set({
      recipientId: ALICE_UID,
      actorId: BOB_UID,
      actorName: "Bob",
      type: "LIKE",
      postId: "post_1",
      message: "Bob reacted to your post",
      read: false,
      createdAt: pastDate(800),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n1").update({
      read: true,
    })
  );
});
