-- =================================================================
-- MIGRATION: Corriger tous les chemins d'images de couverture mal formatés
-- Objectif: Normaliser tous les chemins vers le format /uploads/covers/uuid.ext
-- Date: 2026-04-09
-- =================================================================

-- 0. Nettoyage général: trim, quotes, backslashes Windows
UPDATE courses
SET cover_image = TRIM(BOTH ' ' FROM cover_image)
WHERE cover_image IS NOT NULL;

UPDATE courses
SET cover_image = TRIM(BOTH '"' FROM TRIM(BOTH "'" FROM cover_image))
WHERE cover_image IS NOT NULL
  AND (cover_image LIKE '"%' OR cover_image LIKE "'%");

UPDATE courses
SET cover_image = REPLACE(cover_image, '\\', '/')
WHERE cover_image IS NOT NULL
  AND cover_image LIKE '%\\%';

-- 0.b Extraire /uploads/... depuis les chemins absolus legacy
UPDATE courses
SET cover_image = SUBSTRING(cover_image, LOCATE('/uploads/', LOWER(cover_image)))
WHERE cover_image IS NOT NULL
  AND LOCATE('/uploads/', LOWER(cover_image)) > 1
  AND cover_image NOT LIKE 'preset:%'
  AND cover_image NOT LIKE 'http://%'
  AND cover_image NOT LIKE 'https://%'
  AND cover_image NOT LIKE 'data:image%';

UPDATE courses
SET cover_image = CONCAT('/', SUBSTRING(cover_image, LOCATE('uploads/', LOWER(cover_image))))
WHERE cover_image IS NOT NULL
  AND LOCATE('uploads/', LOWER(cover_image)) > 1
  AND LOCATE('/uploads/', LOWER(cover_image)) = 0
  AND cover_image NOT LIKE 'preset:%'
  AND cover_image NOT LIKE 'http://%'
  AND cover_image NOT LIKE 'https://%'
  AND cover_image NOT LIKE 'data:image%';

-- 1. Chemins du format "covers/uuid.jpg" → "/uploads/covers/uuid.jpg"
UPDATE courses
SET cover_image = CONCAT('/uploads/', cover_image)
WHERE cover_image IS NOT NULL
  AND cover_image NOT LIKE 'preset:%'
  AND cover_image NOT LIKE 'http://%'
  AND cover_image NOT LIKE 'https://%'
  AND LOWER(cover_image) NOT LIKE '/uploads/%'
  AND cover_image NOT LIKE 'data:image%'
  AND LOWER(cover_image) LIKE 'covers/%';

-- 2. Chemins du format "uploads/covers/uuid.jpg" → "/uploads/covers/uuid.jpg"
UPDATE courses
SET cover_image = CONCAT('/', cover_image)
WHERE cover_image IS NOT NULL
  AND cover_image NOT LIKE 'preset:%'
  AND cover_image NOT LIKE 'http://%'
  AND cover_image NOT LIKE 'https://%'
  AND LOWER(cover_image) NOT LIKE '/uploads/%'
  AND cover_image NOT LIKE 'data:image%'
  AND LOWER(cover_image) LIKE 'uploads/%';

-- 3. Standardiser la casse du préfixe /uploads/
UPDATE courses
SET cover_image = CONCAT('/uploads/', SUBSTRING(cover_image, 10))
WHERE cover_image IS NOT NULL
  AND LOWER(cover_image) LIKE '/uploads/%'
  AND cover_image NOT LIKE '/uploads/%';

-- 4. Re-appliquer étapes 1 et 2 au cas où le nettoyage aurait nécessité des corrections
UPDATE courses
SET cover_image = CONCAT('/uploads/', cover_image)
WHERE cover_image IS NOT NULL
  AND cover_image NOT LIKE 'preset:%'
  AND cover_image NOT LIKE 'http://%'
  AND cover_image NOT LIKE 'https://%'
  AND LOWER(cover_image) NOT LIKE '/uploads/%'
  AND cover_image NOT LIKE 'data:image%'
  AND LOWER(cover_image) LIKE 'covers/%';

UPDATE courses
SET cover_image = CONCAT('/', cover_image)
WHERE cover_image IS NOT NULL
  AND cover_image NOT LIKE 'preset:%'
  AND cover_image NOT LIKE 'http://%'
  AND cover_image NOT LIKE 'https://%'
  AND LOWER(cover_image) NOT LIKE '/uploads/%'
  AND cover_image NOT LIKE 'data:image%'
  AND LOWER(cover_image) LIKE 'uploads/%';

-- 5. Vérification : afficher les chemins problématiques restants (s'il y en a)
SELECT id, title, cover_image, status
FROM courses
WHERE cover_image IS NOT NULL
  AND cover_image NOT LIKE 'preset:%'
  AND cover_image NOT LIKE 'http://%'
  AND cover_image NOT LIKE 'https://%'
  AND LOWER(cover_image) NOT LIKE '/uploads/%'
  AND cover_image NOT LIKE 'data:image%'
LIMIT 10;

-- Note: Si le résultat de cette dernière requête est non-vide, 
-- il y a d'autres formats de chemin à gérer manuellement.
