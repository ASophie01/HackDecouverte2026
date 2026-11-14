Smarty

#How does it work#

```
Smarty is a grocery tracking app that allows you to keep track of the when your grocery items are going to expire. The user can add an item,
add a the expiration date as well as a reminder of when the item is soon to be expired. They can also scan (or manually input) the item barcode
for it to automatically fill out the item information (name, expiration date, etc.). Smarty will also let you be able to join a family fridge
by inputing a code. Then you would be able to have a shared fridge with your family! Based on the saved items, an AI will suggest a recipe
where the user could cook a meal.
```

# Languages and APIs used #
```
The main programming language used for this project is Kotlin. Firebase is used for authentication, APIs, and login information.
```
# Open Food Facts API #

The Open Food Facts
```
API was used to look at grocery information from scanning or manually writing a barcode (Product name, brand, package size, and image if available)
```
---
# Gemini API through Firebase AI Logic #
```
Generates recipes based on fridge ingredients, expiry dates, hunger, taste preferences, and servings.
```
---
# Firebase Authentication #
```
Takes charge of registrations, logins, and sessions.
```
----
# Cloud Firestore #
```
Stores inventories, shared fridges, memberships, invite codes, and cooking records.
```
----
# Firebase App Check #
```
Protects access to Firebase AI Logic.
```
---
# Google Code Scanner API #
```
Reads grocery barcodes using Google Play services.
```

---
# Preview Of Smarty App #
<img width="288" height="640" alt="image" src="https://github.com/user-attachments/assets/8d85897d-aa4b-4824-bd94-5061457af8cf" />

