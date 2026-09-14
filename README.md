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
4. अभी provider profile local phone storage में भी सुरक्षित रहती है।
5. असली phone number वाली profile से Call और WhatsApp खोले जा सकते हैं।
6. Demo profiles में जानबूझकर fake phone numbers नहीं हैं।

## Backend
Village Companion now uses **Supabase PostgreSQL**.

- Supabase PostgreSQL database
- Row Level Security (RLS)
- Supabase PostgREST Android client
- V1 service catalogue is seeded in `supabase/schema.sql`
- Android client is initialized in `SupabaseClient.kt`
- Provider data migration from local storage to the database will be added next.

The Android app uses a **Supabase publishable key** only. A Supabase secret/service key must never be shipped inside the APK.

## Product principle
V1 का लक्ष्य पहले **लोगों को सही व्यक्ति से जोड़ना** है। Online payment, commission और guaranteed booking को वास्तविक demand validate होने तक टाला गया है।

## Build
- Android: Jetpack Compose
- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3
- Gradle 8.9 (CI)
- minSdk 26 / targetSdk 35
- Version: 0.3.0
- Supabase Kotlin client: 3.0.1

## Planned next phase
- Supabase Auth / phone verification
- Provider listings from PostgreSQL
- Better village/category search
- Provider photo and profile editing
- Ratings, reviews and reporting
- Admin moderation
- Supabase Storage for profile photos

## Planned roles
- Customer
- Service Provider
- Admin

## Developer
Malaramofficial
