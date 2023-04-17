package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
public class TxLevelTest {

    @Autowired
    LevelService service;

    @Test
    void orderTest() {
        //항상 조금 더 구체적인것이 우선순위를 가진다.

        service.write();  //메소드에 readOnly가 false여서 false
        service.read();  //메소드에는 readOnly가 없지만, class 레벨에 readOnly = true가 있어서 true

        //클래스와 메소드에 모두 없고, 상위 인터페이스에 메소드 또는 인터페이스 타입에 @Transactional이 있으면 인터페이스의 메소드에 있는거부터 확인후 없으면 인터페이스 타입을 본다.
        //인터페이스에 @Transactional을 사용할 순 있지만, AOP가 적용되지 않을 수 있으므로, 가급적이면 구체 클래스에 어노테이션을 적용하자.
        //스프링 5.0부터는 인터페이스에서도 잘 적용이 되긴하지만, 공식 가이드에서도 가급적 클래스에 적용하길 권고한다.
    }

    @TestConfiguration
    static class TxLevelTestConfig {
        @Bean
        LevelService levelService() {
            return new LevelService();
        }
    }

    @Slf4j
    @Transactional(readOnly = true)
    static class LevelService {

        @Transactional(readOnly = false)
        public void write() {
            log.info("call write");
            printTxInfo();
        }

        public void read() {
            log.info("call read");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isSynchronizationActive();
            log.info("tx active = {}",txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("tx readOnly = {}",readOnly);
        }
    }
}
