--
-- PostgreSQL database dump
--

\restrict kjCeFcwrKAK7ZIjMo3ovdNUdV3G8q96669JHR10k0hlx5FBoVEfrYKmiigvrp3W

-- Dumped from database version 18.3
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: brief_deliveries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brief_deliveries (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    brief_id uuid NOT NULL,
    lawyer_id uuid NOT NULL,
    status character varying(30) DEFAULT 'SENT'::character varying NOT NULL,
    rejection_reason text,
    sent_at timestamp without time zone DEFAULT now() NOT NULL,
    viewed_at timestamp without time zone,
    responded_at timestamp without time zone
);


--
-- Name: COLUMN brief_deliveries.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.brief_deliveries.id IS 'PK (UUID)';


--
-- Name: COLUMN brief_deliveries.brief_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.brief_deliveries.brief_id IS 'FK -> briefs.id';


--
-- Name: COLUMN brief_deliveries.lawyer_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.brief_deliveries.lawyer_id IS 'FK -> users.id (변호사)';


--
-- Name: COLUMN brief_deliveries.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.brief_deliveries.status IS '전달 상태 (PENDING / ACCEPTED / REJECTED)';


--
-- Name: COLUMN brief_deliveries.rejection_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.brief_deliveries.rejection_reason IS '거절 사유 (nullable: 거절 안 한 경우 null)';


--
-- Name: COLUMN brief_deliveries.sent_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.brief_deliveries.sent_at IS '전달 시간';


--
-- Name: COLUMN brief_deliveries.viewed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.brief_deliveries.viewed_at IS '변호사 열람 시간 (nullable: 미열람 시 null)';


--
-- Name: COLUMN brief_deliveries.responded_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.brief_deliveries.responded_at IS '변호사 응답 시간 (nullable: 미응답 시 null)';


--
-- Name: briefs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.briefs (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    consultation_id uuid NOT NULL,
    user_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    legal_field character varying(30) NOT NULL,
    content text NOT NULL,
    keywords character varying DEFAULT '[]'::jsonb,
    privacy_setting character varying(20) DEFAULT 'PARTIAL'::character varying NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    confirmed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: COLUMN briefs.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.id IS 'PK (UUID)';


--
-- Name: COLUMN briefs.consultation_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.consultation_id IS 'FK -> consultations.id';


--
-- Name: COLUMN briefs.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.user_id IS 'FK -> users.id';


--
-- Name: COLUMN briefs.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.title IS '의뢰서 제목 (AI 생성)';


--
-- Name: COLUMN briefs.legal_field; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.legal_field IS '법률 분야';


--
-- Name: COLUMN briefs.content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.content IS '의뢰서 본문 (AI 생성 줄글 형식)';


--
-- Name: COLUMN briefs.keywords; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.keywords IS 'AI 추출 키워드 (nullable: 추출 실패 시 null)';


--
-- Name: COLUMN briefs.privacy_setting; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.privacy_setting IS '공개 설정 (PUBLIC / PRIVATE)';


--
-- Name: COLUMN briefs.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.status IS '의뢰서 상태 (DRAFT / CONFIRMED / DELIVERED)';


--
-- Name: COLUMN briefs.confirmed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.confirmed_at IS '사용자 확정 시간 (nullable: 확정 전 null)';


--
-- Name: COLUMN briefs.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.created_at IS '생성 시간';


--
-- Name: COLUMN briefs.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.briefs.updated_at IS '수정 시간';


--
-- Name: consultations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.consultations (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    status character varying(30) DEFAULT 'IN_PROGRESS'::character varying NOT NULL,
    chat_session_id character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    primary_field character varying(30),
    confidence character varying(10)
);


--
-- Name: COLUMN consultations.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.consultations.id IS 'PK (UUID)';


--
-- Name: COLUMN consultations.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.consultations.user_id IS 'FK -> users.id';


--
-- Name: COLUMN consultations.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.consultations.status IS '상담 상태 (CLASSIFYING / IN_PROGRESS / COMPLETED / CANCELLED)';


--
-- Name: COLUMN consultations.chat_session_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.consultations.chat_session_id IS 'MongoDB chatSessions 문서 ID (nullable: 세션 생성 전 null)';


--
-- Name: COLUMN consultations.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.consultations.created_at IS '생성 시간';


--
-- Name: COLUMN consultations.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.consultations.updated_at IS '수정 시간';


--
-- Name: COLUMN consultations.primary_field; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.consultations.primary_field IS 'AI 분류된 법률 분야 (nullable: 분류 전 null)';


--
-- Name: COLUMN consultations.confidence; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.consultations.confidence IS 'AI 분류 신뢰도 (nullable: 분류 전 null)';


--
-- Name: form_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.form_templates (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    legal_field character varying(30) NOT NULL,
    field_name character varying(50) NOT NULL,
    label character varying(100) NOT NULL,
    field_type character varying(20) NOT NULL,
    field_order integer NOT NULL,
    required boolean DEFAULT true NOT NULL,
    collect_method character varying(20) DEFAULT 'CHATBOT'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: COLUMN form_templates.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.form_templates.id IS 'PK (UUID)';


--
-- Name: COLUMN form_templates.legal_field; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.form_templates.legal_field IS '법률 분야';


--
-- Name: COLUMN form_templates.field_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.form_templates.field_name IS '필드 키 이름';


--
-- Name: COLUMN form_templates.label; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.form_templates.label IS 'UI 표시용 라벨';


--
-- Name: COLUMN form_templates.field_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.form_templates.field_type IS '입력 타입 (TEXT / SELECT / RADIO / DATE)';


--
-- Name: COLUMN form_templates.field_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.form_templates.field_order IS '표시 순서';


--
-- Name: COLUMN form_templates.required; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.form_templates.required IS '필수 여부 (true/false)';


--
-- Name: COLUMN form_templates.collect_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.form_templates.collect_method IS '수집 방식 (CHATBOT / FORM)';


--
-- Name: COLUMN form_templates.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.form_templates.created_at IS '생성 시간';


--
-- Name: lawyer_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lawyer_profiles (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    specializations character varying DEFAULT '[]'::jsonb,
    experience_years integer DEFAULT 0,
    certifications text,
    bar_association_number character varying(50) NOT NULL,
    verification_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    verified_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: COLUMN lawyer_profiles.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.lawyer_profiles.id IS 'PK (UUID)';


--
-- Name: COLUMN lawyer_profiles.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.lawyer_profiles.user_id IS 'FK -> users.id (UNIQUE, 1:1 관계)';


--
-- Name: COLUMN lawyer_profiles.specializations; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.lawyer_profiles.specializations IS '전문 분야';


--
-- Name: COLUMN lawyer_profiles.experience_years; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.lawyer_profiles.experience_years IS '경력 연수 (nullable: 미입력 가능)';


--
-- Name: COLUMN lawyer_profiles.certifications; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.lawyer_profiles.certifications IS '자격증 (nullable: 없을 수 있음)';


--
-- Name: COLUMN lawyer_profiles.bar_association_number; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.lawyer_profiles.bar_association_number IS '대한변호사협회 등록 번호';


--
-- Name: COLUMN lawyer_profiles.verification_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.lawyer_profiles.verification_status IS '인증 상태 (PENDING / VERIFIED / REJECTED)';


--
-- Name: COLUMN lawyer_profiles.verified_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.lawyer_profiles.verified_at IS '인증 완료 시간 (nullable: 인증 전 null)';


--
-- Name: COLUMN lawyer_profiles.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.lawyer_profiles.created_at IS '생성 시간';


--
-- Name: COLUMN lawyer_profiles.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.lawyer_profiles.updated_at IS '수정 시간';


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    email character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    provider character varying(255) NOT NULL,
    google_id character varying(255) NOT NULL,
    refresh_token character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: COLUMN users.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.id IS 'PK (UUID)';


--
-- Name: COLUMN users.email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.email IS 'Google OAuth 이메일';


--
-- Name: COLUMN users.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.name IS '사용자 이름';


--
-- Name: COLUMN users.role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.role IS '역할 (USER / LAWYER / ADMIN)';


--
-- Name: COLUMN users.provider; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.provider IS 'OAuth 제공자 (GOOGLE)';


--
-- Name: COLUMN users.google_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.google_id IS 'Google OAuth 고유 ID';


--
-- Name: COLUMN users.refresh_token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.refresh_token IS 'JWT 리프레시 토큰 (nullable: 로그아웃 시 null)';


--
-- Name: COLUMN users.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.created_at IS '생성 시간';


--
-- Name: COLUMN users.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.updated_at IS '수정 시간';


--
-- Name: brief_deliveries brief_deliveries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brief_deliveries
    ADD CONSTRAINT brief_deliveries_pkey PRIMARY KEY (id);


--
-- Name: briefs briefs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.briefs
    ADD CONSTRAINT briefs_pkey PRIMARY KEY (id);


--
-- Name: consultations consultations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consultations
    ADD CONSTRAINT consultations_pkey PRIMARY KEY (id);


--
-- Name: form_templates form_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.form_templates
    ADD CONSTRAINT form_templates_pkey PRIMARY KEY (id);


--
-- Name: lawyer_profiles lawyer_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lawyer_profiles
    ADD CONSTRAINT lawyer_profiles_pkey PRIMARY KEY (id);


--
-- Name: lawyer_profiles lawyer_profiles_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lawyer_profiles
    ADD CONSTRAINT lawyer_profiles_user_id_key UNIQUE (user_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_briefs_consultation_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_briefs_consultation_id ON public.briefs USING btree (consultation_id);


--
-- Name: idx_briefs_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_briefs_status ON public.briefs USING btree (status);


--
-- Name: idx_briefs_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_briefs_user_id ON public.briefs USING btree (user_id);


--
-- Name: idx_consultations_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consultations_status ON public.consultations USING btree (status);


--
-- Name: idx_consultations_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_consultations_user_id ON public.consultations USING btree (user_id);


--
-- Name: idx_deliveries_brief_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_deliveries_brief_id ON public.brief_deliveries USING btree (brief_id);


--
-- Name: idx_deliveries_lawyer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_deliveries_lawyer_id ON public.brief_deliveries USING btree (lawyer_id);


--
-- Name: idx_deliveries_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_deliveries_status ON public.brief_deliveries USING btree (status);


--
-- Name: idx_form_templates_legal_field; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_form_templates_legal_field ON public.form_templates USING btree (legal_field);


--
-- Name: idx_lawyer_profiles_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_lawyer_profiles_user_id ON public.lawyer_profiles USING btree (user_id);


--
-- Name: idx_lawyer_profiles_verification; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lawyer_profiles_verification ON public.lawyer_profiles USING btree (verification_status);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_google_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_google_id ON public.users USING btree (google_id);


--
-- Name: idx_users_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role ON public.users USING btree (role);


--
-- Name: brief_deliveries brief_deliveries_brief_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brief_deliveries
    ADD CONSTRAINT brief_deliveries_brief_id_fkey FOREIGN KEY (brief_id) REFERENCES public.briefs(id);


--
-- Name: brief_deliveries brief_deliveries_lawyer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brief_deliveries
    ADD CONSTRAINT brief_deliveries_lawyer_id_fkey FOREIGN KEY (lawyer_id) REFERENCES public.users(id);


--
-- Name: briefs briefs_consultation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.briefs
    ADD CONSTRAINT briefs_consultation_id_fkey FOREIGN KEY (consultation_id) REFERENCES public.consultations(id);


--
-- Name: briefs briefs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.briefs
    ADD CONSTRAINT briefs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: consultations consultations_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.consultations
    ADD CONSTRAINT consultations_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: lawyer_profiles lawyer_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lawyer_profiles
    ADD CONSTRAINT lawyer_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict kjCeFcwrKAK7ZIjMo3ovdNUdV3G8q96669JHR10k0hlx5FBoVEfrYKmiigvrp3W

