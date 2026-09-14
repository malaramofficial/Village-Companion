# गाँव साथी (Village Companion)

**गाँव में काम और सेवा आसानी से खोजें।**

Village Companion is a rural services marketplace for connecting local customers with workers, farmers, mechanics, tradespeople and agricultural machine owners.

## V1 categories
- 🌾 कृषि मजदूर
- 🚜 Tractor / कृषि मशीन
- ⚡ Electrician
- ⚙️ Motor / Pump Repair
- 🚰 Plumber
- 🧱 Mason / Welding

## Current V1 flow
1. ग्राहक सेवा की category चुनता है।
2. ग्राहक गाँव के हिसाब से providers filter करता है।
3. Provider नाम, गाँव, मोबाइल, सेवा और availability दर्ज कर सकता है।
4. Provider profile अभी इसी फोन में local storage में सुरक्षित रहती है।
5. असली phone number वाली profile से Call और WhatsApp खोले जा सकते हैं।
6. Demo profiles में जानबूझकर fake phone numbers नहीं हैं।

## Product principle
V1 का लक्ष्य पहले **लोगों को सही व्यक्ति से जोड़ना** है। Online payment, commission और guaranteed booking को वास्तविक demand validate होने तक टाला गया है।

## Build
- Android: Jetpack Compose
- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3
- Gradle 8.9 (CI)
- minSdk 24 / targetSdk 35
- Version: 0.2.0

## Planned next phase
- Firebase Authentication / phone verification
- Firestore provider marketplace
- बेहतर village/category search
- Provider photo और profile editing
- Ratings, reviews और reporting
- Admin moderation

## Planned roles
- Customer
- Service Provider
- Admin

## Developer
Malaramofficial
