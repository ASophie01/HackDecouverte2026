import { readFileSync } from 'node:fs';
import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, collection, getDoc, getDocs, setDoc, updateDoc, deleteDoc, writeBatch, runTransaction, serverTimestamp } from 'firebase/firestore';
let env;
const rules=readFileSync(new URL('../firestore.rules', import.meta.url),'utf8');
const owner='alice', guest='bob', outsider='eve';
const code='A'.repeat(32), fridge='family';
const stock={id:'milk',name:'Milk',quantity:1000,unit:'mL',expiryDate:'2026-12-01',category:'',barcode:'',imageUrl:''};
function db(uid){return env.authenticatedContext(uid).firestore()}
async function create(database,id,uid,invite=code,name='Family'){
 const batch=writeBatch(database);
 batch.set(doc(database,`fridges/${id}`),{name,ownerId:uid,inviteCode:invite});
 batch.set(doc(database,`fridges/${id}/members/${uid}`),{userId:uid,inviteCode:invite});
 batch.set(doc(database,`fridgeInvites/${invite}`),{fridgeId:id,name,ownerId:uid});
 batch.set(doc(database,`users/${uid}/fridges/${id}`),{name,ownerId:uid});
 return batch.commit();
}
async function join(database,uid,invite){
 return runTransaction(database,async tx=>{
  const link=await tx.get(doc(database,`fridgeInvites/${invite}`));
  if(!link.exists()) throw Error('Invalid code');
  const data=link.data(), member=doc(database,`fridges/${data.fridgeId}/members/${uid}`);
  const existing=await tx.get(member);
  if(!existing.exists()){
   tx.set(member,{userId:uid,inviteCode:invite});
   tx.set(doc(database,`users/${uid}/fridges/${data.fridgeId}`),{name:data.name,ownerId:data.ownerId});
  }
  return data.fridgeId;
 });
}
before(async()=>{
 env=await initializeTestEnvironment({projectId:'demo-smarty-shared',firestore:{host:'127.0.0.1',port:8089,rules}});
 await env.clearFirestore();
});
after(async()=>{await env.cleanup()});
test('owner creates fridge, membership, invite and index atomically',async()=>{
 await assertSucceeds(create(db(owner),fridge,owner));
 await assertSucceeds(setDoc(doc(db(owner),`fridges/${fridge}/items/milk`),stock));
 assert.equal((await getDocs(collection(db(owner),`users/${owner}/fridges`))).size,1);
});
test('unauthenticated and non-members cannot read or edit inventory',async()=>{
 await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(),`fridges/${fridge}/items/milk`)));
 await assertFails(getDoc(doc(db(outsider),`fridges/${fridge}`)));
 await assertFails(getDocs(collection(db(outsider),`fridges/${fridge}/items`)));
 await assertFails(updateDoc(doc(db(outsider),`fridges/${fridge}/items/milk`),{quantity:50}));
 await assertFails(getDoc(doc(db(outsider),`users/${owner}/fridges/${fridge}`)));
});
test('wrong codes and forged indexes cannot grant membership; invitations cannot be listed',async()=>{
 await assertFails(getDocs(collection(db(outsider),'fridgeInvites')));
 await assert.rejects(join(db(outsider),outsider,'B'.repeat(32)));
 await assertFails(setDoc(doc(db(outsider),`fridges/${fridge}/members/${outsider}`),{userId:outsider,inviteCode:'B'.repeat(32)}));
 await assertFails(setDoc(doc(db(outsider),`users/${outsider}/fridges/${fridge}`),{name:'Family',ownerId:owner}));
});
test('valid code joins idempotently; member reads and edits same inventory',async()=>{
 assert.equal(await join(db(guest),guest,code),fridge);
 assert.equal(await join(db(guest),guest,code),fridge);
 await assertSucceeds(getDocs(collection(db(guest),`fridges/${fridge}/items`)));
 await assertSucceeds(updateDoc(doc(db(guest),`fridges/${fridge}/items/milk`),{quantity:900}));
 assert.equal((await getDoc(doc(db(owner),`fridges/${fridge}/items/milk`))).data().quantity,900);
});
test('members cannot forge others memberships, change ownership, alter invites or remove owner',async()=>{
 await assertFails(setDoc(doc(db(guest),`fridges/${fridge}/members/mallory`),{userId:'mallory',inviteCode:code}));
 await assertFails(updateDoc(doc(db(guest),`fridges/${fridge}`),{ownerId:guest}));
 await assertFails(updateDoc(doc(db(guest),`fridgeInvites/${code}`),{fridgeId:'other'}));
 await assertFails(deleteDoc(doc(db(guest),`fridges/${fridge}/members/${owner}`)));
});
test('invalid item quantities rejected by server',async()=>{
 for(const quantity of [-1,0,NaN,Infinity]) await assertFails(updateDoc(doc(db(guest),`fridges/${fridge}/items/milk`),{quantity}));
});
async function cook(database,uid,receiptId,amount){
 return runTransaction(database,async tx=>{
  const receipt=doc(database,`fridges/${fridge}/cookedRecipes/${receiptId}`);
  if((await tx.get(receipt)).exists())return false;
  const item=doc(database,`fridges/${fridge}/items/milk`);
  const quantity=(await tx.get(item)).data().quantity;
  if(quantity<amount)throw Error('Not enough');
  tx.update(item,{quantity:quantity-amount});
  tx.set(receipt,{cookedBy:uid,title:'Recipe',servings:1,cookedAt:serverTimestamp()});
  return true;
 });
}
test('concurrent identical confirmations deduct exactly once',async()=>{
 const results=await Promise.all([cook(db(owner),owner,'same-recipe',200),cook(db(guest),guest,'same-recipe',200)]);
 assert.deepEqual(results.sort(),[false,true]);
 assert.equal((await getDoc(doc(db(owner),`fridges/${fridge}/items/milk`))).data().quantity,700);
 await assertFails(deleteDoc(doc(db(owner),`fridges/${fridge}/cookedRecipes/same-recipe`)));
});
test('concurrent different recipes cannot overdraw inventory',async()=>{
 const results=await Promise.allSettled([cook(db(owner),owner,'recipe-a',500),cook(db(guest),guest,'recipe-b',500)]);
 assert.equal(results.filter(r=>r.status==='fulfilled').length,1);
 assert.equal((await getDoc(doc(db(owner),`fridges/${fridge}/items/milk`))).data().quantity,200);
});
test('migration retains originals and marker prevents repeated import',async()=>{
 const uid='migrator', database=db(uid), id='personal', invite='C'.repeat(32);
 await env.withSecurityRulesDisabled(async context=>{await setDoc(doc(context.firestore(),`users/${uid}/items/milk`),stock)});
 async function migrate(){
  return runTransaction(database,async tx=>{
   const profile=doc(database,`users/${uid}`);
   if((await tx.get(profile)).exists())return false;
   const old=(await tx.get(doc(database,`users/${uid}/items/milk`))).data();
   tx.set(doc(database,`fridges/${id}`),{name:'Personal',ownerId:uid,inviteCode:invite});
   tx.set(doc(database,`fridges/${id}/members/${uid}`),{userId:uid,inviteCode:invite});
   tx.set(doc(database,`fridgeInvites/${invite}`),{fridgeId:id,name:'Personal',ownerId:uid});
   tx.set(doc(database,`users/${uid}/fridges/${id}`),{name:'Personal',ownerId:uid});
   tx.set(doc(database,`fridges/${id}/items/milk`),old);
   tx.set(profile,{personalFridgeId:id,legacyImportedAt:serverTimestamp()});
   return true;
  });
 }
 assert.equal(await migrate(),true);
 await updateDoc(doc(database,`fridges/${id}/items/milk`),{quantity:300});
 assert.equal(await migrate(),false);
 assert.equal((await getDoc(doc(database,`fridges/${id}/items/milk`))).data().quantity,300);
 assert.equal((await getDoc(doc(database,`users/${uid}/items/milk`))).data().quantity,1000);
});
