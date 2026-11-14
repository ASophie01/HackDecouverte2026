# Email verification and shared-fridge roles

This change keeps the existing collection paths, item schema, invitation format,
personal-fridge migration and atomic creation/join operations.

## Compatibility and roles

- Every Firestore client request now requires an authenticated, verified email.
- Existing memberships without `role` retain editor rights. Newly joined members
  without `role` also start as editors, preserving SharedFridgeService behaviour.
- The owner is determined by the fridge's immutable `ownerId`, not a member role.
- `editor`: read, add, edit, delete inventory and record cooking.
- `adder`: read and add inventory only. Cannot edit/delete even their own items.
- `viewer`: read only.
- `revoked`: no inventory, fridge or cooking access. The user can still read
  their own membership document and private fridge index.
- Only the owner can update another member's `role`. Other membership fields are
  immutable. Neither owner removal nor client ownership transfer is permitted.
- Shared cooking records require editor/owner rights because cooking deducts stock.

## Assign roles

For initial testing, an administrator can edit the `role` string in Firebase Console:
`fridges/{fridgeId}/members/{memberUid}`. Use editor, adder, viewer or revoked.
Console admin writes bypass these rules: test access through clients/emulators.

A future Android member-management screen can issue, as the signed-in owner:

```kotlin
FirebaseFirestore.getInstance().collection("fridges").document(fridgeId)
    .collection("members").document(memberUid)
    .update("role", "viewer")
```

Revoke with `role = revoked`, NOT by deleting the document. The retained document
prevents its user from joining again with the existing invitation. Only an owner
can restore an active role. A revoked user may have downloaded prior data already.

The current invite remains reusable, visible to members, and grants editor access
to new users. Role-based invitations, invite expiry and rotation are not implemented.
The current Android inventory UI still shows editing controls to readers/adders;
Firestore refuses prohibited actions. Add permission-aware UI separately.

## Email verification

SmartyApp now gates SharedFridgeHome behind email verification and a refreshed
ID token. Users can send/resend the email (60-second UI cooldown), refresh after
opening its link, or sign out. Verification is not required again on every launch.
Firebase server quotas remain the authoritative protection against email abuse.

Existing unverified accounts must verify before accessing any Firestore data.
Test with a real test account before deploying rules; do not fake verification
in user-editable Firestore profile fields.

## Before publication

1. Run the rules tests against demo-smarty-shared with Firebase Emulator Suite.
   Existing regression tests cover atomic creation, joining, migration and cooking;
   added cases cover email verification, roles, privilege escalation and revocation.
2. Compile/run the app in Android Studio. Check sending a verification email,
   following its link and refreshing the session before personal-fridge setup.
3. Test owner, editor, adder and viewer with distinct accounts/devices.
4. Publish firestore.rules only after the compatible Android update is ready.

Commands from Smarty, with Node/npm, Firebase CLI and a supported Java installed:

```powershell
npm --prefix tests install
firebase emulators:exec --only firestore --project demo-smarty-shared "npm --prefix tests test"
./gradlew.bat :app:compileDebugKotlin
```

No Firebase rules deployment or Git push is performed by this change.
