package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Slf4j
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    LogRepository logRepository;

    /**
     * memberService    @Transactional : off
     * memberRepository @Transactional : on
     * logRepository    @Transactional : on
     */
    @Test
    void outerTxOff_success() {
        //given
        String username = "outerTxOff_success";

        //when
        memberService.joinV1(username);

        //then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService    @Transactional : off
     * memberRepository @Transactional : on
     * logRepository    @Transactional : on exception
     */
    @Test
    void outerTxOff_fail() {
        //given
        String username = "로그예외_outerTxOff_fail";

        //when
        ;
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        //then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * memberService    @Transactional : on
     * memberRepository @Transactional : off
     * logRepository    @Transactional : off
     */
    @Test
    void singleTx() {
        //given
        String username = "singleTx";

        //when
        memberService.joinV1(username);

        //then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService    @Transactional : on
     * memberRepository @Transactional : on
     * logRepository    @Transactional : on
     */
    @Test
    void outerTxOn_success() {
        //given
        String username = "outerTxOn_success";

        //when
        memberService.joinV1(username);

        //then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService    @Transactional : on
     * memberRepository @Transactional : on
     * logRepository    @Transactional : on exception
     */
    @Test
    void outerTxOn_fail() {
        //given
        String username = "로그예외_outerTxOn_fail";

        //when
        ;
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        //then
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());

        //flow
        //클라이언트A가 MemberService 를 호출하면서 트랜잭션 AOP가 호출된다.
        //여기서 신규 트랜잭션이 생성되고, 물리 트랜잭션도 시작한다.
        //MemberRepository 를 호출하면서 트랜잭션 AOP가 호출된다.
        //이미 트랜잭션이 있으므로 기존 트랜잭션에 참여한다.
        //MemberRepository 의 로직 호출이 끝나고 정상 응답하면 트랜잭션 AOP가 호출된다.
        //트랜잭션 AOP는 정상 응답이므로 트랜잭션 매니저에 커밋을 요청한다. 이 경우 신규 트랜잭션이 아니므로 실제 커밋을 호출하지 않는다.
        //LogRepository 를 호출하면서 트랜잭션 AOP가 호출된다.
        //이미 트랜잭션이 있으므로 기존 트랜잭션에 참여한다.
        //LogRepository 로직에서 런타임 예외가 발생한다. 예외를 던지면 트랜잭션 AOP가 해당 예외를 받게 된다.
        //트랜잭션 AOP는 런타임 예외가 발생했으므로 트랜잭션 매니저에 롤백을 요청한다. 이 경우 신규
        //트랜잭션이 아니므로 물리 롤백을 호출하지는 않는다. 대신에 rollbackOnly 를 설정한다.
        //LogRepository 가 예외를 던졌기 때문에 트랜잭션 AOP도 해당 예외를 그대로 밖으로 던진다.
        //MemberService 에서도 런타임 예외를 받게 되는데, 여기 로직에서는 해당 런타임 예외를 처리하지 않고 밖으로 던진다.
        //트랜잭션 AOP는 런타임 예외가 발생했으므로 트랜잭션 매니저에 롤백을 요청한다. 이 경우 신규
        //트랜잭션이므로 물리 롤백을 호출한다.
        //참고로 이 경우 어차피 롤백이 되었기 때문에, rollbackOnly 설정은 참고하지 않는다.
        //MemberService 가 예외를 던졌기 때문에 트랜잭션 AOP도 해당 예외를 그대로 밖으로 던진다.
        //클라이언트A는 LogRepository 부터 넘어온 런타임 예외를 받게 된다.
    }
}