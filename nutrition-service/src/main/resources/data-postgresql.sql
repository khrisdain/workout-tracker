-- =====================================================================
-- PulseTrack Nutrition Service - sample meal plans, PostgreSQL (qa profile)
--
-- Resolved from spring.sql.init.platform, so each profile carries its own
-- initialisation script. The figures below were produced by the same
-- MacroCalculator the API uses, so the seeded rows are internally
-- consistent with anything created through POST /api/meal-plans.
-- =====================================================================

INSERT INTO meal_plans (member_username, member_name, goal, activity_level, weight_kg, height_cm, age, sex, weekly_training_minutes, notes, basal_metabolic_rate, maintenance_calories, daily_calories, protein_grams, carb_grams, fat_grams, hydration_litres, summary, created_at, updated_at)
SELECT 'jordan', 'Jordan Reyes', 'MAINTAIN', 'MODERATE', 78.4, 176, 27, 'UNSPECIFIED', 210, 'Half marathon block, keep energy steady.', 1671, 2740, 2740, 141, 353, 85, 2.9, '2740 kcal a day, level with an estimated 2740 kcal maintenance. Protein 141g, carbohydrate 353g, fat 85g, based on 210 minutes of training in the last week.', (NOW() - INTERVAL '6 days'), (NOW() - INTERVAL '6 days')
WHERE NOT EXISTS (SELECT 1 FROM meal_plans WHERE member_username = 'jordan');

INSERT INTO meal_plans (member_username, member_name, goal, activity_level, weight_kg, height_cm, age, sex, weekly_training_minutes, notes, basal_metabolic_rate, maintenance_calories, daily_calories, protein_grams, carb_grams, fat_grams, hydration_litres, summary, created_at, updated_at)
SELECT 'priya', 'Priya Raghavan', 'CUT', 'HIGH', 57.2, 161, 23, 'FEMALE', 285, 'Vegetarian, needs the protein target spelled out.', 1302, 2450, 2009, 126, 250, 56, 2.4, '2009 kcal a day, a deficit against an estimated 2450 kcal maintenance. Protein 126g, carbohydrate 250g, fat 56g, based on 285 minutes of training in the last week.', (NOW() - INTERVAL '4 days'), (NOW() - INTERVAL '4 days')
WHERE NOT EXISTS (SELECT 1 FROM meal_plans WHERE member_username = 'priya');

INSERT INTO meal_plans (member_username, member_name, goal, activity_level, weight_kg, height_cm, age, sex, weekly_training_minutes, notes, basal_metabolic_rate, maintenance_calories, daily_calories, protein_grams, carb_grams, fat_grams, hydration_litres, summary, created_at, updated_at)
SELECT 'tomas', 'Tomas Novak', 'BULK', 'MODERATE', 94.1, 189, 36, 'MALE', 180, 'Adding size for the off season, no dairy.', 1947, 3146, 3524, 188, 472, 98, 3.4, '3524 kcal a day, a surplus over an estimated 3146 kcal maintenance. Protein 188g, carbohydrate 472g, fat 98g, based on 180 minutes of training in the last week.', (NOW() - INTERVAL '3 days'), (NOW() - INTERVAL '3 days')
WHERE NOT EXISTS (SELECT 1 FROM meal_plans WHERE member_username = 'tomas');

INSERT INTO meal_plans (member_username, member_name, goal, activity_level, weight_kg, height_cm, age, sex, weekly_training_minutes, notes, basal_metabolic_rate, maintenance_calories, daily_calories, protein_grams, carb_grams, fat_grams, hydration_litres, summary, created_at, updated_at)
SELECT 'leila', 'Leila Haddad', 'CUT', 'LIGHT', 61.8, 165, 31, 'FEMALE', 95, 'Short on time most weeks, wants simple meals.', 1333, 1901, 1559, 136, 157, 43, 2.2, '1559 kcal a day, a deficit against an estimated 1901 kcal maintenance. Protein 136g, carbohydrate 157g, fat 43g, based on 95 minutes of training in the last week.', (NOW() - INTERVAL '2 days'), (NOW() - INTERVAL '2 days')
WHERE NOT EXISTS (SELECT 1 FROM meal_plans WHERE member_username = 'leila');

INSERT INTO meal_plans (member_username, member_name, goal, activity_level, weight_kg, height_cm, age, sex, weekly_training_minutes, notes, basal_metabolic_rate, maintenance_calories, daily_calories, protein_grams, carb_grams, fat_grams, hydration_litres, summary, created_at, updated_at)
SELECT 'coach.b', 'Ben Aritonang', 'MAINTAIN', 'ATHLETE', 86.5, 181, 41, 'MALE', 420, 'Coaching plus own training, appetite is the limiter.', 1796, 3712, 3712, 156, 513, 115, 3.6, '3712 kcal a day, level with an estimated 3712 kcal maintenance. Protein 156g, carbohydrate 513g, fat 115g, based on 420 minutes of training in the last week.', (NOW() - INTERVAL '1 days'), (NOW() - INTERVAL '1 days')
WHERE NOT EXISTS (SELECT 1 FROM meal_plans WHERE member_username = 'coach.b');
