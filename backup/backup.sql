-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: new_voucher_db
-- ------------------------------------------------------
-- Server version	8.0.45-0ubuntu0.24.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `bills`
--

DROP TABLE IF EXISTS `bills`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bills` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `total_amount` decimal(19,2) NOT NULL,
  `user_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKk8vs7ac9xknv5xp18pdiehpp1` (`user_id`),
  KEY `idx_bills_tenant_id` (`tenant_id`),
  CONSTRAINT `fk_bills_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `FKk8vs7ac9xknv5xp18pdiehpp1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bills`
--

LOCK TABLES `bills` WRITE;
/*!40000 ALTER TABLE `bills` DISABLE KEYS */;
INSERT INTO `bills` VALUES (1,'2026-02-18 10:43:57.606123',10000.00,2,2),(2,'2026-02-18 10:49:20.293119',500.00,2,2),(3,'2026-02-20 09:12:30.916968',1000.00,3,2),(4,'2026-03-07 09:59:18.479894',5000.00,6,2),(5,'2026-03-16 08:25:15.039078',2000.00,9,3);
/*!40000 ALTER TABLE `bills` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','<< Flyway Baseline >>','BASELINE','<< Flyway Baseline >>',NULL,'root','2026-03-03 11:01:40',0,1),(2,'2','multitenancy bootstrap','SQL','V2__multitenancy_bootstrap.sql',183530443,'root','2026-03-03 11:38:12',951,1),(3,'3','tenant audit logs','SQL','V3__tenant_audit_logs.sql',359456363,'root','2026-03-03 11:38:13',56,1),(4,'4','tenant voucher inventory','SQL','V4__tenant_voucher_inventory.sql',-1818072751,'root','2026-03-06 05:43:55',262,1),(5,'5','tenant custom voucher requests','SQL','V5__tenant_custom_voucher_requests.sql',1498205521,'root','2026-03-06 06:04:47',178,1),(6,'6','report jobs','SQL','V6__report_jobs.sql',1987622855,'root','2026-03-07 09:23:19',129,1),(7,'7','tenant onboarding requests','SQL','V7__tenant_onboarding_requests.sql',1283662573,'root','2026-03-09 08:03:23',130,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `redemption_history`
--

DROP TABLE IF EXISTS `redemption_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `redemption_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `redeemed_amount` decimal(19,2) NOT NULL,
  `redeemed_at` datetime(6) NOT NULL,
  `remaining_balance_after` decimal(19,2) NOT NULL,
  `bill_id` bigint DEFAULT NULL,
  `user_voucher_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKl6merswdxakmfe8qj05a6tynd` (`bill_id`),
  KEY `FKjo7u7nbgf28p43hlllgkiwwxh` (`user_voucher_id`),
  KEY `idx_redemption_history_tenant_id` (`tenant_id`),
  CONSTRAINT `fk_redemption_history_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `FKjo7u7nbgf28p43hlllgkiwwxh` FOREIGN KEY (`user_voucher_id`) REFERENCES `user_vouchers` (`id`),
  CONSTRAINT `FKl6merswdxakmfe8qj05a6tynd` FOREIGN KEY (`bill_id`) REFERENCES `bills` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `redemption_history`
--

LOCK TABLES `redemption_history` WRITE;
/*!40000 ALTER TABLE `redemption_history` DISABLE KEYS */;
INSERT INTO `redemption_history` VALUES (1,1000.00,'2026-02-18 10:45:43.965047',0.00,1,3,2),(2,100.00,'2026-02-18 10:47:19.591955',0.00,1,1,2),(3,500.00,'2026-02-18 10:51:14.147321',500.00,2,4,2),(4,500.00,'2026-02-20 09:15:04.655652',0.00,3,5,2),(5,1000.00,'2026-02-20 09:28:00.623014',0.00,3,6,2),(6,2500.00,'2026-03-07 09:59:44.624717',0.00,4,9,2),(7,2000.00,'2026-03-16 08:31:12.257201',500.00,5,10,3);
/*!40000 ALTER TABLE `redemption_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `refresh_tokens`
--

DROP TABLE IF EXISTS `refresh_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `replaced_by_token_hash` varchar(64) DEFAULT NULL,
  `revoked` bit(1) NOT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `token_hash` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_refresh_token_hash` (`token_hash`),
  KEY `idx_refresh_user_id` (`user_id`),
  CONSTRAINT `FK1lih5y2npsf8u5o3vhdb9y0os` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refresh_tokens`
--

LOCK TABLES `refresh_tokens` WRITE;
/*!40000 ALTER TABLE `refresh_tokens` DISABLE KEYS */;
INSERT INTO `refresh_tokens` VALUES (1,'2026-02-19 08:51:49.026223','2026-03-05 08:51:49.026182',NULL,_binary '\0',NULL,'537a2dc47081181f079e7c6dac167bf3ab1497529d746d655c5dab535acf6c00',1),(2,'2026-02-19 08:55:01.404560','2026-03-05 08:55:01.404520',NULL,_binary '\0',NULL,'01b347f3432bedb49f0e341e63457a000b3ae01c193c6bddf065364e90e1220e',1),(3,'2026-02-19 08:56:06.047396','2026-03-05 08:56:06.047375',NULL,_binary '\0',NULL,'89101d4194ea570671f4e0a45cef41f97f1b50ac89b2b70870eefb3176695cf4',1),(4,'2026-02-19 09:00:41.251835','2026-03-05 09:00:41.251809',NULL,_binary '\0',NULL,'687f37b86bb14060af52640b1ff1f38b0968fb8106b3851980aa7e4aa2f2494e',1),(5,'2026-02-19 09:02:35.734200','2026-03-05 09:02:35.734173',NULL,_binary '\0',NULL,'7e4ddf28cc8aa1a56a755ae0508a9776f9b1acefdcb59d4db4ca4c799c720c77',1),(6,'2026-02-19 09:06:45.250402','2026-03-05 09:06:45.250388','4ee4e264a85568c973f0e5b3d3f9eaef6e8374b1ceb646da5e3e74205c495b6c',_binary '','2026-02-19 09:07:36.098328','f27ef8cfe303bb4dbe36dac7e20e70fa9695cefba84ed60ae6e83b11e62a3dd9',1),(7,'2026-02-19 09:07:36.099637','2026-03-05 09:07:36.099615',NULL,_binary '\0',NULL,'4ee4e264a85568c973f0e5b3d3f9eaef6e8374b1ceb646da5e3e74205c495b6c',1),(8,'2026-02-19 09:15:56.410829','2026-03-05 09:15:56.410800','4948a4acd6356f4d389bcd4c9d426e393b3ae32733186be4b24eb5635dcbd635',_binary '','2026-02-19 09:18:22.628487','2529e8f78ed834ee400b5a1f056781329019527960893f8b41da8ffab6b99924',1),(9,'2026-02-19 09:18:22.628734','2026-03-05 09:18:22.628717',NULL,_binary '\0',NULL,'4948a4acd6356f4d389bcd4c9d426e393b3ae32733186be4b24eb5635dcbd635',1),(10,'2026-02-19 11:57:36.991158','2026-03-05 11:57:36.991133',NULL,_binary '\0',NULL,'e44848480f805170d17e9384b0f52c72b7dcb4a9c55491bb3b2cf95ba44b3e88',1),(11,'2026-02-19 11:57:46.400245','2026-03-05 11:57:46.400232',NULL,_binary '\0',NULL,'42c94953146518a3904cb48073279fed5ebe9992c1c53e3c237398df038a4eaf',1),(12,'2026-02-19 11:58:22.802154','2026-03-05 11:58:22.802141',NULL,_binary '\0',NULL,'74ff25e80d6a2ed784e7b510474acbf7258f5a1d3997bc9827d697c1d84d8410',1),(13,'2026-02-19 11:59:20.226756','2026-03-05 11:59:20.226708',NULL,_binary '','2026-02-19 12:00:21.218688','2b1e2484cc45b1fd10e410c79e55142366d75d8afe94c8e14a5991092d468f96',1),(14,'2026-02-19 12:33:10.779800','2026-03-05 12:33:10.779782',NULL,_binary '','2026-02-19 12:34:15.735843','ea83b3da28e993b0fa16c69120bad9664f34dc067fe607af27d12eea5ecfef65',1),(15,'2026-02-19 12:34:30.928082','2026-03-05 12:34:30.928030','2c8f28280537210819d8791680102b28289937cabd807d2417dbcad2c9ad0cbd',_binary '','2026-02-19 12:35:12.153903','68f93a31270f18db42139bd2b8bd5890ba3ba2dbd150a3ae22640ece6e775060',1),(16,'2026-02-19 12:35:12.154322','2026-03-05 12:35:12.154303',NULL,_binary '\0',NULL,'2c8f28280537210819d8791680102b28289937cabd807d2417dbcad2c9ad0cbd',1),(17,'2026-02-19 12:37:52.853391','2026-03-05 12:37:52.853367','2d982e9ae338ffae92b9d596395eba238b4fe05eab6bb3f48833c275a41aef64',_binary '','2026-02-19 12:38:42.723698','6a8dec88c5a006948112df00a3ea2e1a7f948f31f919fa0e241ac96df740933a',1),(18,'2026-02-19 12:38:42.724188','2026-03-05 12:38:42.724165','76d2658a6c05c6460b033b557b17e36cfa7f9e220513218b70a7008b0b48c9a5',_binary '','2026-02-19 12:41:35.125783','2d982e9ae338ffae92b9d596395eba238b4fe05eab6bb3f48833c275a41aef64',1),(19,'2026-02-19 12:41:35.126172','2026-03-05 12:41:35.126146',NULL,_binary '','2026-02-19 12:44:11.687644','76d2658a6c05c6460b033b557b17e36cfa7f9e220513218b70a7008b0b48c9a5',1),(20,'2026-02-20 08:43:17.860823','2026-03-06 08:43:17.860798','b0e6390f3ebd3fc8bd434d659376372663129704c7dad3ee4dd33410572243a9',_binary '','2026-02-20 08:55:05.693213','432538ea9f2af96d3a7796c92ed43509b6b996364f6e3c8ae129146940b3fd07',1),(21,'2026-02-20 08:55:05.694444','2026-03-06 08:55:05.694433',NULL,_binary '\0',NULL,'b0e6390f3ebd3fc8bd434d659376372663129704c7dad3ee4dd33410572243a9',1),(22,'2026-02-20 08:58:14.380187','2026-03-06 08:58:14.380165',NULL,_binary '\0',NULL,'327a9ca799ae06138395d948b2ed5530268ff540e0fb34257ad74298d5b9ab1f',2),(23,'2026-02-20 09:05:18.358702','2026-03-06 09:05:18.358682',NULL,_binary '\0',NULL,'798235aa89897be4cd22bdef9f8df20f5134bbf209ab01fd1f2a46b6890aaa05',4),(24,'2026-02-20 09:06:31.147908','2026-03-06 09:06:31.147884','3f75bce3da5e9da9ad156049b14da37f5dce05a8ea0f04faee5900458272062d',_binary '','2026-02-20 09:20:27.271813','502379e22bd1f9c32241adeb7d63240b8f9b119a346e219fa5a7be22b68bd101',3),(25,'2026-02-20 09:11:45.544752','2026-03-06 09:11:45.544738',NULL,_binary '\0',NULL,'9bfb1bb29bf6b487525f3b78937b584d8722664927d6a24aee36efdad8efdb70',1),(26,'2026-02-20 09:20:27.272147','2026-03-06 09:20:27.272130',NULL,_binary '\0',NULL,'3f75bce3da5e9da9ad156049b14da37f5dce05a8ea0f04faee5900458272062d',3),(27,'2026-02-20 09:26:46.581349','2026-03-06 09:26:46.581323',NULL,_binary '\0',NULL,'17d72b9def645c7dc70bd5e74b95925a2bca295e1b1a4bc01a52be53475fad72',3),(28,'2026-02-20 10:34:41.251836','2026-03-06 10:34:41.251810','dcab45de679d36439841dc4d1c413dedf303da3ea0d391890eeb60a8e2dd4e21',_binary '','2026-02-20 11:10:52.094530','fbc31930e980686b4b3b74d57902d9b6aa41f0ca58070ea43172b752e33b2963',1),(29,'2026-02-20 11:10:52.095996','2026-03-06 11:10:52.095981','87cfa396e20ef9b075eff1c7cf9867a1453f38e0bb7edd7bb5e503848f7e4214',_binary '','2026-02-20 11:55:31.931006','dcab45de679d36439841dc4d1c413dedf303da3ea0d391890eeb60a8e2dd4e21',1),(30,'2026-02-20 11:11:38.497685','2026-03-06 11:11:38.497670',NULL,_binary '\0',NULL,'df8175aa8c2e5ebb991e1c36a9aacc8aa68ecd850ebd3bc20bd3ff5709a749ff',3),(31,'2026-02-20 11:12:21.774494','2026-03-06 11:12:21.774480',NULL,_binary '\0',NULL,'f660f9a25daf3522fad38a8a476b5b2cc385501ccda61924e3f549badfc65da4',3),(32,'2026-02-20 11:55:31.931815','2026-03-06 11:55:31.931792',NULL,_binary '\0',NULL,'87cfa396e20ef9b075eff1c7cf9867a1453f38e0bb7edd7bb5e503848f7e4214',1),(33,'2026-02-20 11:56:59.453723','2026-03-06 11:56:59.453702','16d960795ec0fc21600c647c6ddff7e93bc1b6ea30e85bb54cdcaf2e7002cb5e',_binary '','2026-02-20 12:11:11.442477','12726b4fd675ab8f9807f4fb246caa0e1177615752d60d137d1494118f093a06',3),(34,'2026-02-20 12:11:11.442697','2026-03-06 12:11:11.442686',NULL,_binary '\0',NULL,'16d960795ec0fc21600c647c6ddff7e93bc1b6ea30e85bb54cdcaf2e7002cb5e',3),(35,'2026-02-20 12:12:31.953557','2026-03-06 12:12:31.953541',NULL,_binary '\0',NULL,'3098f5ac421ac08f95a45b62c751383b7f382889961c11f0e26fa40967851310',1),(36,'2026-02-20 12:19:55.400709','2026-03-06 12:19:55.400694',NULL,_binary '\0',NULL,'5fd27e3f577196ab7dbefa47343ca78d1b0579dc9e382bd3450a4aaf4bc9b9b7',1),(37,'2026-02-20 12:22:29.836438','2026-03-06 12:22:29.836399',NULL,_binary '\0',NULL,'620dff65eece37725ec3139826af20d22169c6b04e8cc6c603e42d3817085cf1',3);
/*!40000 ALTER TABLE `refresh_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report_jobs`
--

DROP TABLE IF EXISTS `report_jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_jobs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_type` varchar(32) NOT NULL,
  `target_type` varchar(32) NOT NULL,
  `target_id` bigint NOT NULL,
  `requested_by_user_id` bigint NOT NULL,
  `from_date` date NOT NULL,
  `to_date` date NOT NULL,
  `recipient_email` varchar(255) NOT NULL,
  `status` varchar(32) NOT NULL,
  `output_file_path` varchar(1000) DEFAULT NULL,
  `error_message` varchar(2000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `completed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_report_jobs_status_created_at` (`status`,`created_at`),
  KEY `idx_report_jobs_target` (`target_type`,`target_id`),
  KEY `idx_report_jobs_requested_by` (`requested_by_user_id`,`created_at`),
  CONSTRAINT `fk_report_jobs_requested_by_user` FOREIGN KEY (`requested_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report_jobs`
--

LOCK TABLES `report_jobs` WRITE;
/*!40000 ALTER TABLE `report_jobs` DISABLE KEYS */;
INSERT INTO `report_jobs` VALUES (1,'USER_DETAILED','USER',6,6,'2026-03-07','2026-03-07','mishrasatyam2060@gmail.com','QUEUED',NULL,NULL,'2026-03-07 10:19:25.594341',NULL),(2,'USER_DETAILED','USER',6,6,'2026-03-07','2026-03-09','mishrasatyam2060@gmail.com,satyammishra0323@gmail.com','COMPLETED','./generated-reports/report-2.pdf',NULL,'2026-03-09 05:09:32.408271','2026-03-09 05:09:40.806790'),(3,'TENANT_DETAILED','TENANT',3,5,'2026-03-03','2026-03-09','mishra@azilen.com,satyammishra0323@gmail.com','COMPLETED','./generated-reports/report-3.pdf',NULL,'2026-03-09 05:27:33.683990','2026-03-09 05:27:45.431273'),(4,'TENANT_DETAILED','TENANT',3,5,'2026-03-03','2026-03-09','mishra@azilen.com,satyammishra0323@gmail.com','COMPLETED','./generated-reports/report-4.pdf',NULL,'2026-03-09 05:27:40.208377','2026-03-09 05:27:47.346486'),(5,'TENANT_DETAILED','TENANT',3,5,'2026-03-03','2026-03-09','mishra@azilen.com,satyammishra0323@gmail.com','QUEUED',NULL,NULL,'2026-03-09 05:44:00.420171',NULL),(6,'USER_DETAILED','USER',6,6,'2026-03-07','2026-03-09','mishrasatyam2060@gmail.com,satyammishra0323@gmail.com','QUEUED',NULL,NULL,'2026-03-09 05:44:11.938887',NULL),(7,'TENANT_DETAILED','TENANT',3,5,'2026-03-03','2026-03-09','mishra@azilen.com,satyammishra0323@gmail.com','IN_PROGRESS',NULL,NULL,'2026-03-09 05:52:43.473051',NULL),(8,'TENANT_DETAILED','TENANT',3,5,'2026-03-03','2026-03-09','mishra@azilen.com,satyammishra0323@gmail.com','IN_PROGRESS',NULL,NULL,'2026-03-09 05:58:33.877880',NULL),(9,'TENANT_DETAILED','TENANT',3,5,'2026-03-03','2026-03-09','mishra@azilen.com,satyammishra0323@gmail.com','IN_PROGRESS',NULL,NULL,'2026-03-09 06:02:04.548765',NULL),(10,'USER_DETAILED','USER',7,7,'2026-03-01','2026-03-09','mail.debug.20260309@example.com,satyammishra0323@gmail.com','COMPLETED','./generated-reports/report-10.pdf',NULL,'2026-03-09 06:24:14.791761','2026-03-09 06:24:23.838464'),(11,'TENANT_DETAILED','TENANT',3,5,'2026-03-03','2026-03-09','mishra@azilen.com,satyammishra0323@gmail.com','COMPLETED','./generated-reports/report-11.pdf',NULL,'2026-03-09 06:29:30.085289','2026-03-09 06:29:37.532057'),(12,'USER_DETAILED','USER',6,6,'2026-03-07','2026-03-09','mishrasatyam2060@gmail.com,satyammishra0323@gmail.com','FAILED',NULL,'ExecutionException: java.lang.RuntimeException: Failed to generate PDF report: Could not initialize proxy [com.example.Voucher.entity.VoucherTemplate#5] - no session | caused by: RuntimeException: Failed to generate PDF report: Could not initialize proxy [com.example.Voucher.entity.VoucherTemplate#5] - no session | caused by: LazyInitializationException: Could not initialize proxy [com.example.Voucher.entity.VoucherTemplate#5] - no session','2026-03-09 06:30:14.818104','2026-03-09 06:30:14.869704'),(13,'USER_DETAILED','USER',6,6,'2026-03-07','2026-03-09','mishrasatyam2060@gmail.com,satyammishra0323@gmail.com','FAILED',NULL,'ExecutionException: java.lang.RuntimeException: Failed to generate PDF report: Could not initialize proxy [com.example.Voucher.entity.VoucherTemplate#5] - no session | caused by: RuntimeException: Failed to generate PDF report: Could not initialize proxy [com.example.Voucher.entity.VoucherTemplate#5] - no session | caused by: LazyInitializationException: Could not initialize proxy [com.example.Voucher.entity.VoucherTemplate#5] - no session','2026-03-09 06:31:56.053955','2026-03-09 06:31:56.094267'),(14,'USER_DETAILED','USER',6,6,'2026-03-07','2026-03-09','mishrasatyam2060@gmail.com,satyammishra0323@gmail.com','COMPLETED','./generated-reports/report-14.pdf',NULL,'2026-03-09 06:34:37.909027','2026-03-09 06:34:47.931054'),(15,'TENANT_DETAILED','TENANT',3,5,'2026-03-03','2026-03-16','mishra@azilen.com,satyammishra0323@gmail.com','COMPLETED','./generated-reports/report-15.pdf',NULL,'2026-03-16 08:35:37.824965','2026-03-16 08:35:47.404311');
/*!40000 ALTER TABLE `report_jobs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'Platform administrator','PLATFORM_ADMIN'),(2,'Standard user','USER'),(4,'Tenant administrator','TENANT_ADMIN');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_audit_logs`
--

DROP TABLE IF EXISTS `tenant_audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `actor_user_id` bigint DEFAULT NULL,
  `action` varchar(100) NOT NULL,
  `details` varchar(2000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `fk_tenant_audit_logs_actor` (`actor_user_id`),
  KEY `idx_tenant_audit_logs_tenant_id` (`tenant_id`),
  KEY `idx_tenant_audit_logs_created_at` (`created_at`),
  CONSTRAINT `fk_tenant_audit_logs_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_tenant_audit_logs_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_audit_logs`
--

LOCK TABLES `tenant_audit_logs` WRITE;
/*!40000 ALTER TABLE `tenant_audit_logs` DISABLE KEYS */;
INSERT INTO `tenant_audit_logs` VALUES (1,2,1,'TENANT_ACTIVATED','string','2026-03-03 11:53:25.916138'),(2,3,1,'TENANT_ONBOARDED','Organization tenant created with tenant admin userId=5','2026-03-03 11:55:02.699718'),(3,3,1,'TENANT_ACTIVATED','testing','2026-03-03 12:22:55.037501'),(4,4,1,'TENANT_ONBOARDING_APPROVED','Onboarding requestId=2, tenantAdminUserId=8','2026-03-09 08:20:33.951309');
/*!40000 ALTER TABLE `tenant_audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_onboarding_requests`
--

DROP TABLE IF EXISTS `tenant_onboarding_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_onboarding_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_name` varchar(255) NOT NULL,
  `tenant_code` varchar(100) NOT NULL,
  `admin_first_name` varchar(255) NOT NULL,
  `admin_last_name` varchar(255) NOT NULL,
  `admin_email` varchar(255) NOT NULL,
  `admin_phone_number` varchar(20) NOT NULL,
  `admin_password_hash` varchar(255) NOT NULL,
  `notes` varchar(2000) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `review_comment` varchar(2000) DEFAULT NULL,
  `reviewed_by_user_id` bigint DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `approved_tenant_id` bigint DEFAULT NULL,
  `approved_tenant_admin_user_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_tenant_onboarding_requests_status_created_at` (`status`,`created_at`),
  KEY `idx_tenant_onboarding_requests_tenant_code` (`tenant_code`),
  KEY `idx_tenant_onboarding_requests_admin_email` (`admin_email`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_onboarding_requests`
--

LOCK TABLES `tenant_onboarding_requests` WRITE;
/*!40000 ALTER TABLE `tenant_onboarding_requests` DISABLE KEYS */;
INSERT INTO `tenant_onboarding_requests` VALUES (1,'TCS','12345','Tata','consultancy','satyam@tcs.com','6544853341','$2a$10$r0h8w0684eIT4dyoDHSqCeuTwZHZdehiAaNzBxe4nkbPPPbzUv9Xe',NULL,'REJECTED','welcome',1,'2026-03-09 08:13:48.209673',NULL,NULL,'2026-03-09 08:10:13.206455'),(2,'TCS','12345','Tata','consultancy','satyam@tcs.com','6544853341','$2a$10$7oqgyLPUw9ahTPDatn0gWOzhKVjcSntEKa7VMn/e4gpbsFaEx4tiW',NULL,'APPROVED','welcome',1,'2026-03-09 08:20:33.949442',4,8,'2026-03-09 08:14:15.306412');
/*!40000 ALTER TABLE `tenant_onboarding_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_voucher_distributions`
--

DROP TABLE IF EXISTS `tenant_voucher_distributions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_voucher_distributions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `tenant_voucher_inventory_id` bigint NOT NULL,
  `distributed_to_user_id` bigint NOT NULL,
  `distributed_by_user_id` bigint NOT NULL,
  `quantity_distributed` int NOT NULL,
  `total_distributed_amount` decimal(19,2) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `fk_tenant_voucher_distributions_inventory` (`tenant_voucher_inventory_id`),
  KEY `fk_tenant_voucher_distributions_to_user` (`distributed_to_user_id`),
  KEY `fk_tenant_voucher_distributions_by_user` (`distributed_by_user_id`),
  KEY `idx_tenant_voucher_distributions_tenant_id` (`tenant_id`),
  KEY `idx_tenant_voucher_distributions_created_at` (`created_at`),
  CONSTRAINT `fk_tenant_voucher_distributions_by_user` FOREIGN KEY (`distributed_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_tenant_voucher_distributions_inventory` FOREIGN KEY (`tenant_voucher_inventory_id`) REFERENCES `tenant_voucher_inventory` (`id`),
  CONSTRAINT `fk_tenant_voucher_distributions_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `fk_tenant_voucher_distributions_to_user` FOREIGN KEY (`distributed_to_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_voucher_distributions`
--

LOCK TABLES `tenant_voucher_distributions` WRITE;
/*!40000 ALTER TABLE `tenant_voucher_distributions` DISABLE KEYS */;
INSERT INTO `tenant_voucher_distributions` VALUES (1,3,1,9,5,10,2500.00,'2026-03-16 08:22:09.316537');
/*!40000 ALTER TABLE `tenant_voucher_distributions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_voucher_inventory`
--

DROP TABLE IF EXISTS `tenant_voucher_inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_voucher_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `voucher_template_id` bigint NOT NULL,
  `quantity_purchased_total` int NOT NULL,
  `quantity_available` int NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_voucher_inventory_tenant_template` (`tenant_id`,`voucher_template_id`),
  KEY `idx_tenant_voucher_inventory_tenant_id` (`tenant_id`),
  KEY `idx_tenant_voucher_inventory_template_id` (`voucher_template_id`),
  CONSTRAINT `fk_tenant_voucher_inventory_template` FOREIGN KEY (`voucher_template_id`) REFERENCES `voucher_templates` (`id`),
  CONSTRAINT `fk_tenant_voucher_inventory_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_voucher_inventory`
--

LOCK TABLES `tenant_voucher_inventory` WRITE;
/*!40000 ALTER TABLE `tenant_voucher_inventory` DISABLE KEYS */;
INSERT INTO `tenant_voucher_inventory` VALUES (1,3,5,110,100,'2026-03-06 10:53:00.938074','2026-03-16 08:22:09.309484');
/*!40000 ALTER TABLE `tenant_voucher_inventory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant_voucher_requests`
--

DROP TABLE IF EXISTS `tenant_voucher_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_voucher_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `requested_by_user_id` bigint NOT NULL,
  `requested_voucher_code` varchar(255) NOT NULL,
  `requested_unit_value` decimal(19,2) NOT NULL,
  `requested_start_date` date NOT NULL,
  `requested_expiry_date` date NOT NULL,
  `notes` varchar(2000) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `platform_comment` varchar(1000) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `fk_tenant_voucher_requests_requested_by` (`requested_by_user_id`),
  KEY `idx_tenant_voucher_requests_tenant_id` (`tenant_id`),
  KEY `idx_tenant_voucher_requests_status` (`status`),
  KEY `idx_tenant_voucher_requests_created_at` (`created_at`),
  CONSTRAINT `fk_tenant_voucher_requests_requested_by` FOREIGN KEY (`requested_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_tenant_voucher_requests_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant_voucher_requests`
--

LOCK TABLES `tenant_voucher_requests` WRITE;
/*!40000 ALTER TABLE `tenant_voucher_requests` DISABLE KEYS */;
INSERT INTO `tenant_voucher_requests` VALUES (1,3,5,'azilen50',50.00,'2026-03-06','2026-03-07','string','PENDING',NULL,'2026-03-06 10:43:40.071332','2026-03-06 10:43:40.071332');
/*!40000 ALTER TABLE `tenant_voucher_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenants`
--

DROP TABLE IF EXISTS `tenants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenants` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(100) NOT NULL,
  `tenant_name` varchar(255) NOT NULL,
  `tenant_type` varchar(32) NOT NULL,
  `active` bit(1) NOT NULL DEFAULT b'1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenants_tenant_code` (`tenant_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenants`
--

LOCK TABLES `tenants` WRITE;
/*!40000 ALTER TABLE `tenants` DISABLE KEYS */;
INSERT INTO `tenants` VALUES (2,'SYSTEM_INDIVIDUAL','System Individual Customers','SYSTEM_INDIVIDUAL',_binary '','2026-03-03 17:08:12.062521'),(3,'123','Azilen','ORGANIZATION',_binary '','2026-03-03 11:55:02.580286'),(4,'12345','TCS','ORGANIZATION',_binary '','2026-03-09 08:20:33.912556');
/*!40000 ALTER TABLE `tenants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transactions`
--

DROP TABLE IF EXISTS `transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `final_amount` decimal(19,2) NOT NULL,
  `total_amount` decimal(19,2) NOT NULL,
  `bill_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKq5xcgnnqi7dyywyihf7uimanw` (`bill_id`),
  KEY `FKqwv7rmvc8va8rep7piikrojds` (`user_id`),
  KEY `idx_transactions_tenant_id` (`tenant_id`),
  CONSTRAINT `fk_transactions_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `FKq5xcgnnqi7dyywyihf7uimanw` FOREIGN KEY (`bill_id`) REFERENCES `bills` (`id`),
  CONSTRAINT `FKqwv7rmvc8va8rep7piikrojds` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transactions`
--

LOCK TABLES `transactions` WRITE;
/*!40000 ALTER TABLE `transactions` DISABLE KEYS */;
INSERT INTO `transactions` VALUES (1,'2026-02-18 10:45:43.969487',9000.00,10000.00,1,2,2),(2,'2026-02-18 10:47:19.595293',9900.00,10000.00,1,2,2),(3,'2026-02-18 10:51:14.149066',0.00,500.00,2,2,2),(4,'2026-02-20 09:15:04.658940',500.00,1000.00,3,3,2),(5,'2026-02-20 09:28:00.626058',0.00,1000.00,3,3,2),(6,'2026-03-07 09:59:44.629789',2500.00,5000.00,4,6,2),(7,'2026-03-16 08:31:12.261137',0.00,2000.00,5,9,3);
/*!40000 ALTER TABLE `transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `FKh8ciramu9cc9q3qcqiv4ue8a6` (`role_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (1,1),(1,2),(2,2),(3,2),(4,2),(6,2),(7,2),(9,2),(5,4),(8,4);
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_vouchers`
--

DROP TABLE IF EXISTS `user_vouchers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_vouchers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `purchased_at` datetime(6) NOT NULL,
  `quantity_purchased` int NOT NULL,
  `remaining_balance` decimal(19,2) NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `total_purchased_amount` decimal(19,2) NOT NULL,
  `user_id` bigint NOT NULL,
  `voucher_template_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK90ahc2var0yrghyxr9tapdokg` (`user_id`),
  KEY `FKc8dqic71f47a9a8vmoaxeco82` (`voucher_template_id`),
  KEY `idx_user_vouchers_tenant_id` (`tenant_id`),
  CONSTRAINT `FK90ahc2var0yrghyxr9tapdokg` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_user_vouchers_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `FKc8dqic71f47a9a8vmoaxeco82` FOREIGN KEY (`voucher_template_id`) REFERENCES `voucher_templates` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_vouchers`
--

LOCK TABLES `user_vouchers` WRITE;
/*!40000 ALTER TABLE `user_vouchers` DISABLE KEYS */;
INSERT INTO `user_vouchers` VALUES (1,'2026-02-18 07:35:22.901717',10,0.00,'INACTIVE',100.00,2,1,2),(2,'2026-02-18 08:20:26.594517',10,1000.00,'ACTIVE',1000.00,1,2,2),(3,'2026-02-18 10:40:19.893806',10,0.00,'INACTIVE',1000.00,2,2,2),(4,'2026-02-18 10:50:51.901682',10,500.00,'ACTIVE',1000.00,2,3,2),(5,'2026-02-20 09:09:54.338004',5,0.00,'INACTIVE',500.00,3,3,2),(6,'2026-02-20 09:21:27.803143',5,0.00,'INACTIVE',1000.00,3,4,2),(7,'2026-02-20 11:58:48.479897',10,2500.00,'ACTIVE',2500.00,3,5,2),(8,'2026-02-20 12:23:34.936845',10,1500.00,'ACTIVE',1500.00,3,6,2),(9,'2026-03-07 09:57:55.422335',10,0.00,'INACTIVE',2500.00,6,5,2),(10,'2026-03-16 08:22:09.312493',10,500.00,'ACTIVE',2500.00,9,5,3);
/*!40000 ALTER TABLE `user_vouchers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone_number` varchar(255) NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK9q63snka3mdh91as4io72espi` (`phone_number`),
  KEY `idx_users_tenant_id` (`tenant_id`),
  CONSTRAINT `fk_users_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-02-18 06:31:17.371659','admin@gmail.com',_binary '','Admin','user','$2a$10$EmjcK2opHGWIsLa7DrQWeu8jYoJQej0fi7uDaRBshxh5AfHmIuBSO','1275874202',2),(2,'2026-02-18 07:28:34.426933','userone@gmail.com',_binary '','user','one','$2a$10$F61OaBL5Z7r.D3kFK0l4FeO9QC2urIJ3dMCcH4vXZyTsDiH.BgbCK','0433226945',2),(3,'2026-02-20 09:04:09.967494','biki@gmail.com',_binary '','biki','shah','$2a$10$/EQD5DxZNH1krD.7rpWsEeNR9iNaGh2NCVHzHXobZCppnUQDu5M1m','8910128602',2),(4,'2026-02-20 09:05:18.103843','user1771578318@example.com',_binary '','Test','User','$2a$10$/VljomdRhAWT93tJlallluAVJ8RVr9gnFkHAZN7mZ04yWH6CbLo8S','9771578318',2),(5,'2026-03-03 11:55:02.693487','mishra@azilen.com',_binary '','satyam','mishra','$2a$10$wTOPbML77H2yjMCtn9s7B.zui41Ck.b5KsKxhVyebFTH84fG3xaVm','1870666311',3),(6,'2026-03-07 09:54:39.264094','mishrasatyam2060@gmail.com',_binary '','satyam','mishras','$2a$10$f.CeeLTrg14yAqT/BTZ1R.8SCYDW6Texdx.NMOEsszvIfFFf.GOeu','1520388091',2),(7,'2026-03-09 06:23:49.521720','mail.debug.20260309@example.com',_binary '','Mail','Debug','$2a$10$qqpRupkodvBEFXJ38jvtO.2W1OaqnYL0ndLuVhYBDgmqIMOUPZK26','9876501234',2),(8,'2026-03-09 08:20:33.934848','satyam@tcs.com',_binary '','Tata','consultancy','$2a$10$7oqgyLPUw9ahTPDatn0gWOzhKVjcSntEKa7VMn/e4gpbsFaEx4tiW','6544853341',4),(9,'2026-03-16 08:15:11.926915','tenantuser@azilen.com',_binary '','tenant','user','$2a$10$cbZ8GUbsh9qPnMZAoQyYfOhEVQIgoztaIhLIjh.yrH7EJZxmsMCSm','9272420783',3);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `voucher_templates`
--

DROP TABLE IF EXISTS `voucher_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voucher_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `expiry_date` date NOT NULL,
  `start_date` date NOT NULL,
  `unit_value` decimal(19,2) NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3s0rj6eanamhbkdd0hds8qgwj` (`code`),
  KEY `idx_voucher_templates_tenant_id` (`tenant_id`),
  CONSTRAINT `fk_voucher_templates_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `voucher_templates`
--

LOCK TABLES `voucher_templates` WRITE;
/*!40000 ALTER TABLE `voucher_templates` DISABLE KEYS */;
INSERT INTO `voucher_templates` VALUES (1,'save10','2026-02-18 07:34:30.424866',_binary '','2026-02-28','2026-02-18',10.00,2),(2,'flat100','2026-02-18 08:11:06.097673',_binary '','2026-02-18','2026-02-18',100.00,2),(3,'flatsave100','2026-02-18 08:11:29.242928',_binary '','2026-03-18','2026-02-18',100.00,2),(4,'flat200','2026-02-20 08:53:05.758756',_binary '','2026-02-20','2026-02-20',200.00,2),(5,'flat250','2026-02-20 11:56:26.623618',_binary '','2026-03-20','2026-02-20',250.00,2),(6,'flat150','2026-02-20 12:21:45.302812',_binary '','2026-03-20','2026-02-20',150.00,2),(7,'azilen10','2026-03-06 05:31:45.421599',_binary '','2026-03-06','2026-03-06',10.00,3);
/*!40000 ALTER TABLE `voucher_templates` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-31 14:21:47
