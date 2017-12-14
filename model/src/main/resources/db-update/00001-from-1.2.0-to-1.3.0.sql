--
-- JBoss, Home of Professional Open Source.
-- Copyright 2014 Red Hat, Inc., and individual contributors
-- as indicated by the @author tags.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- TargetRepository

create sequence target_repository_repo_id_seq;
-- ## Updates required by new artifact repository relations
create table TargetRepository (
    id integer not null,
    identifier varchar(255) not null,
    repositoryPath varchar(255) not null,
    repositoryType varchar(255) not null,
    primary key (id),
    unique (identifier, repositoryPath)
);
-- insert default repositories
insert into TargetRepository (id, identifier, repositoryPath, repositoryType) values (1, 'indy-maven', 'builds-untested', 'MAVEN');
insert into TargetRepository (id, identifier, repositoryPath, repositoryType) values (2, 'indy-maven', 'builds-untested-temp', 'MAVEN_TEMPORARY');
insert into TargetRepository (id, identifier, repositoryPath, repositoryType) values (3, 'indy-maven', 'shared-imports', 'MAVEN');
insert into TargetRepository (id, identifier, repositoryPath, repositoryType) values (4, 'indy-maven', 'shared-imports', 'MAVEN_TEMPORARY');
insert into TargetRepository (id, identifier, repositoryPath, repositoryType) values (5, 'indy-http', '', 'GENERIC_PROXY');

-- Start the sequence id for target repository from last value = 6
-- We need to set last_value to 6 and not to 5 because the sequence has column
-- 'is_called' set to false
-- https://www.postgresql.org/docs/8.1/static/functions-sequence.html
alter sequence target_repository_repo_id_seq restart with 6;

-- Artifact
alter table Artifact add targetRepository_id integer;

alter table Artifact
    add constraint fk_artifact_targetRepository
    foreign key (targetRepository_id)
    references TargetRepository;

-- migrate data
-- old repotype 0 -> maven (1)
-- old repotype 3 -> generic proxy (5)
update Artifact set targetRepository_id = 1 where repotype = 0;
update Artifact set targetRepository_id = 5 where repotype = 3;

alter table Artifact alter column targetRepository_id set not null;
alter table Artifact drop column repotype;

-- temporary build flags
alter table buildrecord add temporarybuild boolean;
update buildrecord set temporarybuild = true;
alter table buildrecord alter column temporarybuild set not null;

alter table buildconfigsetrecord add temporarybuild boolean;
update buildconfigsetrecord set temporarybuild = true;
alter table buildconfigsetrecord alter column temporarybuild set not null;


-- BuildRecordPushResult
create sequence build_record_push_result_id_seq;

create table BuildRecordPushResult (
    id int4 not null,
    brewBuildId int4,
    brewBuildUrl varchar(255),
    log text,
    status varchar(255),
    buildRecord_id int4,
    primary key (id)
);

alter table BuildRecordPushResult
    add constraint fk_buildrecordpushresult_buildrecord
    foreign key (buildRecord_id)
    references BuildRecord;

-- Copy data from build_record_attributes table to empty buildrecordpushresult table
INSERT INTO
    buildrecordpushresult (id, buildrecord_id, brewbuildid, brewbuildurl, status)
SELECT
    nextval('build_record_push_result_id_seq'), brID.build_record_id, cast(brID.value as int), brURL.value, 'SUCCESS'
FROM
    build_record_attributes brID
left join
    build_record_attributes brURL on brID.build_record_id = brURL.build_record_id AND brURL.key = 'brewLink'
WHERE
    brID.key = 'brewId';

-- build_config_set_record_attributes collection table
create table build_config_set_record_attributes (
    build_config_set_record_id int4 not null,
    value varchar(255),
    key varchar(255) not null,
    primary key (build_config_set_record_id, key)
);

alter table build_config_set_record_attributes
    add constraint fk_build_config_set_record_attributes_build_config_set_record
    foreign key (build_config_set_record_id)
    references BuildConfigSetRecord;