-- Material propio del usuario (perfil): se usa para autorrellenar el material
-- al unirte a una quedada (sigue siendo editable ahí para esa quedada concreta).
-- Mismo formato JSON simple que meetup_members.gear_json:
-- {"cuerda":true,"grigri":false,"cintas":12,"crashpads":2}
ALTER TABLE users ADD COLUMN IF NOT EXISTS gear_json VARCHAR(512);
