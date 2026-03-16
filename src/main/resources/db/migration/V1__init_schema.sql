CREATE TYPE "public"."class_status" AS ENUM('active', 'inactive', 'archived');
CREATE TYPE "public"."role" AS ENUM('student', 'teacher', 'admin');

CREATE TABLE "classes" (
    "id" integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "subject_id" integer NOT NULL,
    "teacher_id" text NOT NULL,
    "invite_code" varchar(50) NOT NULL,
    "name" varchar(255) NOT NULL,
    "banner_cld_pub_id" text,
    "banner_url" text,
    "capacity" integer DEFAULT 50 NOT NULL,
    "description" text,
    "status" "class_status" DEFAULT 'active' NOT NULL,
    "schedules" jsonb NOT NULL,
    "created_at" timestamp DEFAULT now() NOT NULL,
    "updated_at" timestamp DEFAULT now() NOT NULL,
    CONSTRAINT "classes_invite_code_unique" UNIQUE("invite_code")
);

CREATE TABLE "departments" (
    "id" integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "code" varchar(50) NOT NULL,
    "name" varchar(255) NOT NULL,
    "description" text,
    "created_at" timestamp DEFAULT now() NOT NULL,
    "updated_at" timestamp DEFAULT now() NOT NULL,
    CONSTRAINT "departments_code_unique" UNIQUE("code")
);

CREATE TABLE "enrollments" (
    "id" integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "student_id" text NOT NULL,
    "class_id" integer NOT NULL,
    "created_at" timestamp DEFAULT now() NOT NULL,
    "updated_at" timestamp DEFAULT now() NOT NULL
);

CREATE TABLE "subjects" (
    "id" integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "department_id" integer NOT NULL,
    "name" varchar(255) NOT NULL,
    "code" varchar(50) NOT NULL,
    "description" text,
    "created_at" timestamp DEFAULT now() NOT NULL,
    "updated_at" timestamp DEFAULT now() NOT NULL,
    CONSTRAINT "subjects_code_unique" UNIQUE("code")
);

CREATE TABLE "account" (
    "id" text PRIMARY KEY NOT NULL,
    "user_id" text NOT NULL,
    "account_id" text NOT NULL,
    "provider_id" text NOT NULL,
    "access_token" text,
    "refresh_token" text,
    "access_token_expires_at" timestamp,
    "refresh_token_expires_at" timestamp,
    "scope" text,
    "id_token" text,
    "password" text,
    "created_at" timestamp DEFAULT now() NOT NULL,
    "updated_at" timestamp DEFAULT now() NOT NULL
);

CREATE TABLE "session" (
    "id" text PRIMARY KEY NOT NULL,
    "user_id" text NOT NULL,
    "token" text NOT NULL,
    "expires_at" timestamp NOT NULL,
    "ip_address" text,
    "user_agent" text,
    "created_at" timestamp DEFAULT now() NOT NULL,
    "updated_at" timestamp DEFAULT now() NOT NULL
);

CREATE TABLE "user" (
    "id" text PRIMARY KEY NOT NULL,
    "name" text NOT NULL,
    "email" text NOT NULL,
    "email_verified" boolean NOT NULL,
    "image" text,
    "role" "role" DEFAULT 'student' NOT NULL,
    "image_cld_pub_id" text,
    "created_at" timestamp DEFAULT now() NOT NULL,
    "updated_at" timestamp DEFAULT now() NOT NULL
);

CREATE TABLE "verification" (
    "id" text PRIMARY KEY NOT NULL,
    "identifier" text NOT NULL,
    "value" text NOT NULL,
    "expires_at" timestamp NOT NULL,
    "created_at" timestamp DEFAULT now() NOT NULL,
    "updated_at" timestamp DEFAULT now() NOT NULL
);

ALTER TABLE "classes" ADD CONSTRAINT "classes_subject_id_subjects_id_fk"
    FOREIGN KEY ("subject_id") REFERENCES "public"."subjects"("id") ON DELETE CASCADE;
ALTER TABLE "classes" ADD CONSTRAINT "classes_teacher_id_user_id_fk"
    FOREIGN KEY ("teacher_id") REFERENCES "public"."user"("id") ON DELETE RESTRICT;
ALTER TABLE "enrollments" ADD CONSTRAINT "enrollments_student_id_user_id_fk"
    FOREIGN KEY ("student_id") REFERENCES "public"."user"("id") ON DELETE CASCADE;
ALTER TABLE "enrollments" ADD CONSTRAINT "enrollments_class_id_classes_id_fk"
    FOREIGN KEY ("class_id") REFERENCES "public"."classes"("id") ON DELETE CASCADE;
ALTER TABLE "subjects" ADD CONSTRAINT "subjects_department_id_departments_id_fk"
    FOREIGN KEY ("department_id") REFERENCES "public"."departments"("id") ON DELETE RESTRICT;
ALTER TABLE "account" ADD CONSTRAINT "account_user_id_user_id_fk"
    FOREIGN KEY ("user_id") REFERENCES "public"."user"("id") ON DELETE NO ACTION;
ALTER TABLE "session" ADD CONSTRAINT "session_user_id_user_id_fk"
    FOREIGN KEY ("user_id") REFERENCES "public"."user"("id") ON DELETE NO ACTION;

CREATE INDEX "classes_subject_id_idx" ON "classes" USING btree ("subject_id");
CREATE INDEX "classes_teacher_id_idx" ON "classes" USING btree ("teacher_id");
CREATE INDEX "enrollments_student_id_idx" ON "enrollments" USING btree ("student_id");
CREATE INDEX "enrollments_class_id_idx" ON "enrollments" USING btree ("class_id");
CREATE INDEX "enrollments_student_class_unique" ON "enrollments" USING btree ("student_id","class_id");
CREATE INDEX "account_user_id_idx" ON "account" USING btree ("user_id");
CREATE UNIQUE INDEX "account_provider_account_unique" ON "account" USING btree ("provider_id","account_id");
CREATE INDEX "session_user_id_idx" ON "session" USING btree ("user_id");
CREATE UNIQUE INDEX "session_token_unique" ON "session" USING btree ("token");
CREATE INDEX "verification_identifier_idx" ON "verification" USING btree ("identifier");

