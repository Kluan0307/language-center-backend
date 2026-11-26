package com.example.assistant.chat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class BusinessCodeLanguageTest {
 @Test void shortCodesKeepTheirExactDigitsAndCorrectIntent(){var policy=new TopicPolicy();assertEquals(TopicPolicy.Family.PERSONNEL,policy.family("GV007 có bằng cấp gì",TopicPolicy.Family.NONE));assertEquals(TopicPolicy.Family.ACCOUNTS,policy.family("Thông tin NS001",TopicPolicy.Family.NONE));assertTrue(policy.normalize("HV0001 đang học lớp nào").contains("hv0001"));assertFalse(policy.normalize("GV007 có bằng cấp gì").contains("giao vien 7"));}
}
