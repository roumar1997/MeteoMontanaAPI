-- Material que cada participante lleva a la quedada.
-- JSON sencillo: {"crashpads":2} o {"cintas":12,"cuerda":1,"grigri":1}
ALTER TABLE meetup_members ADD COLUMN IF NOT EXISTS gear_json VARCHAR(512);
