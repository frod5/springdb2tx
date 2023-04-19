package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

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

    /**
     * memberService    @Transactional : on
     * memberRepository @Transactional : on
     * logRepository    @Transactional : on exception
     * 실무에서 가장 실수하기 좋은 케이스
     */
    @Test
    void recoverException_fail() {
        //given
        String username = "로그예외_recoverException_fail";

        //when
        ;
        assertThatThrownBy(() -> memberService.joinV2(username))
                .isInstanceOf(UnexpectedRollbackException.class);

        //then
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());

        //flow
        //LogRepository 에서 예외가 발생한다. 예외를 던지면 LogRepository 의 트랜잭션 AOP가 해당 예외를 받는다.
        //신규 트랜잭션이 아니므로 물리 트랜잭션을 롤백하지는 않고, 트랜잭션 동기화 매니저에 rollbackOnly 를 표시한다.
        //이후 트랜잭션 AOP는 전달 받은 예외를 밖으로 던진다.
        //예외가 MemberService 에 던져지고, MemberService 는 해당 예외를 복구한다. 그리고 정상적으로 리턴한다.
        //정상 흐름이 되었으므로 MemberService 의 트랜잭션 AOP는 커밋을 호출한다.
        //커밋을 호출할 때 신규 트랜잭션이므로 실제 물리 트랜잭션을 커밋해야 한다. 이때 rollbackOnly 를 체크한다.
        //rollbackOnly 가 체크 되어 있으므로 물리 트랜잭션을 롤백한다.
        //트랜잭션 매니저는 UnexpectedRollbackException 예외를 던진다.
        //트랜잭션 AOP도 전달받은 UnexpectedRollbackException 을 클라이언트에 던진다.

        //정리
        //논리 트랜잭션 중 하나라도 롤백되면 전체 트랜잭션은 롤백된다.
        //내부 트랜잭션이 롤백 되었는데, 외부 트랜잭션이 커밋되면 UnexpectedRollbackException 예외가 발생한다.
        //rollbackOnly 상황에서 커밋이 발생하면 UnexpectedRollbackException 예외가 발생한다.
    }

    /**
     * memberService    @Transactional : on
     * memberRepository @Transactional : on
     * logRepository    @Transactional : on(REQUIRES_NEW) exception
     */
    @Test
    void recoverException_success() {
        //given
        String username = "로그예외_recoverException_success";

        //when
        memberService.joinV2(username);

        //then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());

        //flow
        //LogRepository 에서 예외가 발생한다. 예외를 던지면 LogRepository 의 트랜잭션 AOP가 해당 예외를 받는다.
        //REQUIRES_NEW 를 사용한 신규 트랜잭션이므로 물리 트랜잭션을 롤백한다. 물리 트랜잭션을 롤백했으므로
        //rollbackOnly 를 표시하지 않는다. 여기서 REQUIRES_NEW 를 사용한 물리 트랜잭션은 롤백되고 완전히 끝이 나버린다.
        //이후 트랜잭션 AOP는 전달 받은 예외를 밖으로 던진다.
        //예외가 MemberService 에 던져지고, MemberService 는 해당 예외를 복구한다. 그리고 정상적으로 리턴한다.
        //정상 흐름이 되었으므로 MemberService 의 트랜잭션 AOP는 커밋을 호출한다.
        //커밋을 호출할 때 신규 트랜잭션이므로 실제 물리 트랜잭션을 커밋해야 한다. 이때 rollbackOnly 를 체크한다.
        //rollbackOnly 가 없으므로 물리 트랜잭션을 커밋한다.
        //이후 정상 흐름이 반환된다.
        //결과적으로 회원 데이터는 저장되고, 로그 데이터만 롤백 되는 것을 확인할 수 있다.

        //정리
        //논리 트랜잭션은 하나라도 롤백되면 관련된 물리 트랜잭션은 롤백되어 버린다.
        //이 문제를 해결하려면 REQUIRES_NEW 를 사용해서 트랜잭션을 분리해야 한다.
        //참고로 예제를 단순화 하기 위해 MemberService 가 MemberRepository , LogRepository 만 호출하지만 실제로는 더 많은 리포지토리들을 호출하고 그 중에 LogRepository 만 트랜잭션을
        //분리한다고 생각해보면 이해하는데 도움이 될 것이다.

        //주의
        //REQUIRES_NEW 를 사용하면 하나의 HTTP 요청에 동시에 2개의 데이터베이스 커넥션을 사용하게 된다.
        //따라서 성능이 중요한 곳에서는 이런 부분을 주의해서 사용해야 한다.
        //REQUIRES_NEW 를 사용하지 않고 문제를 해결할 수 있는 단순한 방법이 있다면, 그 방법을 선택하는 것이 더 좋다.
    }
}