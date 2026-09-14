# Village Companion — Backend Plan

## V1 architecture

Android app → Supabase Auth → PostgreSQL → Supabase Storage (later)

The first backend milestone is deliberately small. The app connects customers with local service providers; it does not process payments or promise a booking.

## Core data model

- `villages`: searchable village master data.
- `services`: the six V1 service categories.
- `profiles`: authentication-linked user identity and role.
- `providers`: the public marketplace listing for a provider.
- `reports`: abuse/fake-profile reports for future moderation.

Relationship summary:

`auth.users 1—1 profiles`

`profiles 1—0..1 providers`

`services 1—many providers`

`villages 1—many providers`

`providers 1—many reports`

## Security decisions

- Row Level Security (RLS) is enabled on every public table.
- Active villages/services/providers can be read by the marketplace.
- A signed-in user can create/update only their own profile and provider listing.
- A signed-in user can report a provider as themselves.
- There is no client-side admin write policy.
- Provider phone numbers are public in V1 because direct Call/WhatsApp is the core contact flow. A later version can add verified contact, anti-spam and privacy controls.

## Intentionally postponed

- Online payments
- Commission calculation
- Ratings/reviews
- Guaranteed booking
- Complex admin dashboard
- Phone OTP (SMS cost)
- Photo uploads
- Multiple services per provider

These should be added only after the basic marketplace flow is stable and real users validate demand.

## Next implementation order

1. Create a Supabase project.
2. Run `supabase/schema.sql` once on the new project.
3. Verify tables, seed services and RLS in Supabase.
4. Add the Supabase Android client using public project URL + anon/publishable key only.
5. Replace local `SharedPreferences` provider save/load with Supabase calls.
6. Add authentication before allowing cloud provider writes.
7. Replace demo results with database search by service + village.
8. Test anonymous/public browsing, authenticated provider registration and provider update.
9. Add Storage only when provider photos are actually needed.
