-- ═══════════════════════════════════════════════════════════════════════
--  FIX: "Data truncated for column 'status'" lors de l'archivage
-- ═══════════════════════════════════════════════════════════════════════
--  Problème: La colonne 'status' de la table 'courses' est peut-être
--  définie comme un ENUM MySQL qui ne contient pas 'ARCHIVED'.
--  
--  Solution: Convertir en VARCHAR(20) pour correspondre à l'entité JPA.
--  
--  EXÉCUTER CETTE REQUÊTE UNE SEULE FOIS dans MySQL Workbench ou phpMyAdmin :
-- ═══════════════════════════════════════════════════════════════════════

ALTER TABLE courses MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

-- Vérification :
-- SHOW COLUMNS FROM courses WHERE Field = 'status';
-- Le type doit maintenant être : varchar(20)
