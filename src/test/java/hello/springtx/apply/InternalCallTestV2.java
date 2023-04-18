package hello.springtx.apply;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InternalCallTestV2 {

    @Autowired
    CallService service;

    @Test
    void printProxy() {
        log.info("callService class = {}",service.getClass());
    }

    @Test
    void callExternalV2() {
        //InternalService class를 별도 분리하여 internal()을 호출하면 InternalService의 프록시가 실제 InternalService의 internal()을 호출하여 트랜잭션이 적용된다.
        //스프링의 트랙잭션 AOP (@Transactional)은 public 메소드에서만 트랜잭션 적용하도록 기본설정 되어 있다.
        //public이 아닌 곳에 @Transactional이 있으면 예외는 발생하지 않고, 트랜잭션을 무시한다.
        service.external();
    }

    @TestConfiguration
    static class InternalCallV1TestConfig {

        @Bean
        CallService callService() {
            return new CallService(internalService());
        }

        @Bean
        InternalService internalService() {
            return new InternalService();
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    static class CallService {

        private final InternalService internalService;

        public void external() {
            log.info("call external");
            printTxInfo();
            internalService.internal();
        }

        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isSynchronizationActive();
            log.info("tx active = {}",txActive);
        }
    }

    @Slf4j
    static class InternalService {
        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isSynchronizationActive();
            log.info("tx active = {}",txActive);
        }
    }
}
