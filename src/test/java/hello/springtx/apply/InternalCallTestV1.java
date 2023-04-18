package hello.springtx.apply;

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
public class InternalCallTestV1 {

    @Autowired
    CallService service;

    @Test
    void printProxy() {
        log.info("callService class = {}",service.getClass());
    }

    @Test
    void callInternal() {
        service.internal();
    }

    @Test
    void callExternal() {
        //external()는 @Transactional이 없어서. 트랜잭션 적용 X
        //external()안에 있는 internal()는 @Transactional이 존재하여 트랜잭션이 활성화가 될것으로 예상.
        //예샹과 다르게 @Transactional이 있는 internal()을 호출하더라도 트랜잭션이 활성화 되지않는다.
        //그 이유는 CallService 내부에는 @Transactional이 존재하여, 트랜잭션 AOP 프록시가 생겨서 프록시가 적용된 CallService가 스프링 빈으로 등록된다.
        //internal()을 호출할떄는 프록시 callService에서 internal()은 @Transactional이 존재하여 실제 트랜잭션 처리 후 실제 callService에 로직을 호출하고. 트랜잭션을 종료 처리한다.
        //external()을 호출하면 프록시 callService에서 external()은 @Transactional이 존재하지 않아 트랜잭션을 처리하지 않고, 실제 callService에 external()을 호출한다.
        //실제 callService의 로직을 호출하기때문에 실제 callService의 external()이 internal()을 호출하여 @Transactional이 있더라도, 프록시를 통해 internal()을 호출한게 아니어서
        //트랜잭션이 적용되지 않는다. (실제 트랜잭션은 프록시로 생성된 callService에서 하기 떄문이다.)
        service.external();
    }

    @TestConfiguration
    static class InternalCallV1TestConfig {

        @Bean
        CallService callService() {
            return new CallService();
        }
    }

    @Slf4j
    static class CallService {
        public void external() {
            log.info("call external");
            printTxInfo();
            internal();
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
}
