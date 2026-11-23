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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user cannot read or write folders or flashcards", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("folders").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("folders")
      .doc("folder_1")
      .set({
        userId: ALICE_UID,
        name: "Trà thất",
        accentTone: "SAGE",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("flashcards").get()
  );
});

test("Owner can create, update, list, and delete folders with Kotlin-aligned payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("folders")
      .doc("folder_1")
      .set({
        userId: ALICE_UID,
        name: "Giao tiếp hàng ngày",
        description: "Các câu chào hỏi cơ bản",
        accentTone: "SAGE",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("folders").get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("folders")
      .doc("folder_1")
      .update({
        name: "Giao tiếp Regency",
        accentTone: "BRONZE",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject ghost field & immutable createdAt change
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("folders")
      .doc("folder_2")
      .set({
        id: "folder_2",
        userId: ALICE_UID,
        name: "Ghost",
        accentTone: "SAGE",
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("folders")
      .doc("folder_1")
      .update({
        createdAt: new Date(Date.now() - 500),
        updatedAt: new Date(Date.now() - 500),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("folders")
      .doc("folder_1")
      .delete()
  );
});

test("Owner can create flashcard inside existing folder, record swipe review, and edit card", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Reject orphan flashcard referencing non-existent folder
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcards")
      .doc("card_orphan")
      .set({
        userId: ALICE_UID,
        folderId: "missing_folder",
        hanzi: "茶",
        pinyin: "chá",
        meaning: "Trà",
        category: "Từ vựng",
        lastResult: "NEW",
        masteryCount: 0,
        lapseCount: 0,
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("folders")
      .doc("folder_1")
      .set({
        userId: ALICE_UID,
        name: "Trà đạo",
        accentTone: "SAGE",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcards")
      .doc("card_1")
      .set({
        userId: ALICE_UID,
        folderId: "folder_1",
        hanzi: "风和日丽",
        pinyin: "fēng hé rì lì",
        meaning: "Gió hòa nắng đẹp",
        category: "Thành ngữ",
        exampleHanzi: "今天风和日丽，适合散步。",
        exampleMeaning: "Hôm nay gió hòa nắng đẹp, rất hợp đi dạo.",
        lastResult: "NEW",
        masteryCount: 0,
        lapseCount: 0,
        createdAt: now,
        updatedAt: now,
      })
  );

  // Review swipe branch
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcards")
      .doc("card_1")
      .update({
        lastResult: "REMEMBERED",
        masteryCount: 1,
        lapseCount: 0,
        lastReviewedAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Edit branch
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcards")
      .doc("card_1")
      .update({
        meaning: "Thời tiết nắng ấm dễ chịu",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Future timestamp must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flashcards")
      .doc("card_1")
      .update({
        lastResult: "FORGOT",
        lapseCount: 1,
        lastReviewedAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );

  // Query matching Kotlin repository
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("flashcards").get()
  );
});

test("Cross-user access is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const now = new Date(Date.now() - 5000);
    await db.collection("users").doc(BOB_UID).collection("folders").doc("b_f1").set({
      userId: BOB_UID,
      name: "Bob Folder",
      accentTone: "SLATE",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(BOB_UID).collection("flashcards").doc("b_c1").set({
      userId: BOB_UID,
      folderId: "b_f1",
      hanzi: "书",
      pinyin: "shū",
      meaning: "Sách",
      category: "Từ vựng",
      lastResult: "NEW",
      masteryCount: 0,
      lapseCount: 0,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("folders").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("flashcards").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("flashcards")
      .doc("b_c1")
      .update({ meaning: "Hacked", updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("folders")
      .doc("b_f1")
      .delete()
  );
});
