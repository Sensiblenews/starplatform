package com.sensible.admin.scheduler;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.sensible.admin.scheduler.AlertRules.Severity;
import com.sensible.admin.scheduler.AlertStateMachine.Kind;
import com.sensible.admin.scheduler.AlertStateMachine.Transition;

/**
 * 경보 상태 전이: 연속 확인·격상·복구·재알림·플래핑 억제.
 * 5분 주기를 가정하고 시각은 분 단위로 넣는다.
 */
public class AlertStateMachineTest {

	private static final long MIN = 60000L;
	private static final long REMIND = 60 * MIN;

	private List<Transition> step(AlertStateMachine m, Severity sev, long minute) {
		return m.apply("disk", sev, 90, "msg", minute * MIN, 2, 2, REMIND, 3);
	}

	@Test
	public void 한_번_관측으로는_열지_않고_두_번_연속이면_OPEN() {
		AlertStateMachine m = new AlertStateMachine();
		assertTrue(step(m, Severity.DANGER, 0).isEmpty());
		List<Transition> t = step(m, Severity.DANGER, 5);
		assertEquals(1, t.size());
		assertEquals(Kind.OPEN, t.get(0).kind);
		assertEquals(Severity.DANGER, t.get(0).severity);
		assertEquals(5 * MIN, t.get(0).openedAt);
		assertEquals(Severity.DANGER, m.state("disk").open);
	}

	@Test
	public void 흔들리는_값은_연속_카운트가_끊겨_열리지_않는다() {
		AlertStateMachine m = new AlertStateMachine();
		step(m, Severity.DANGER, 0);
		step(m, Severity.NORMAL, 5);
		step(m, Severity.DANGER, 10);
		assertTrue(step(m, Severity.NORMAL, 15).isEmpty());
		assertEquals(Severity.NORMAL, m.state("disk").open);
	}

	@Test
	public void 열린_뒤_같은_등급은_아무_전이도_없다() {
		AlertStateMachine m = new AlertStateMachine();
		step(m, Severity.DANGER, 0);
		step(m, Severity.DANGER, 5);
		assertTrue(step(m, Severity.DANGER, 10).isEmpty());
		assertTrue(step(m, Severity.DANGER, 15).isEmpty());
		assertTrue(step(m, Severity.WARNING, 20).isEmpty()); // 낮아지는 건 유지
		assertEquals(Severity.DANGER, m.state("disk").open);
	}

	@Test
	public void 더_높은_등급이_두_번_연속이면_ESCALATE_한_번() {
		AlertStateMachine m = new AlertStateMachine();
		step(m, Severity.WARNING, 0);
		step(m, Severity.WARNING, 5);
		assertTrue(step(m, Severity.CRITICAL, 10).isEmpty());
		List<Transition> t = step(m, Severity.CRITICAL, 15);
		assertEquals(Kind.ESCALATE, t.get(0).kind);
		assertEquals(Severity.WARNING, t.get(0).previousSeverity);
		assertEquals(Severity.CRITICAL, t.get(0).severity);
		assertTrue(step(m, Severity.CRITICAL, 20).isEmpty());
		assertEquals(Severity.CRITICAL, m.state("disk").peak);
	}

	@Test
	public void 정상_두_번_연속이면_RESOLVE_하고_상태가_비워진다() {
		AlertStateMachine m = new AlertStateMachine();
		step(m, Severity.DANGER, 0);
		step(m, Severity.DANGER, 5);
		m.bindAlertId("disk", 77L);
		m.markMailSent("disk", 5 * MIN);
		assertTrue(step(m, Severity.NORMAL, 10).isEmpty());
		List<Transition> t = step(m, Severity.NORMAL, 15);
		assertEquals(Kind.RESOLVE, t.get(0).kind);
		assertEquals(Severity.DANGER, t.get(0).previousSeverity);
		assertEquals(77L, t.get(0).alertId);
		assertTrue(t.get(0).mailSent);
		assertEquals(0L, m.state("disk").alertId);
		assertFalse(m.state("disk").mailSent);
		assertEquals(Severity.NORMAL, m.state("disk").open);
	}

	@Test
	public void 복구_중_다시_나빠지면_정상_카운트가_리셋된다() {
		AlertStateMachine m = new AlertStateMachine();
		step(m, Severity.DANGER, 0);
		step(m, Severity.DANGER, 5);
		step(m, Severity.NORMAL, 10);
		step(m, Severity.DANGER, 15);
		assertTrue(step(m, Severity.NORMAL, 20).isEmpty());
		assertEquals(Severity.DANGER, m.state("disk").open);
	}

	@Test
	public void 긴급_미확인_경보는_간격마다_최대_횟수까지_REMIND() {
		AlertStateMachine m = new AlertStateMachine();
		step(m, Severity.CRITICAL, 0);
		step(m, Severity.CRITICAL, 5);
		m.markMailSent("disk", 5 * MIN);
		assertTrue(step(m, Severity.CRITICAL, 30).isEmpty()); // 아직 60분 안 됨
		assertEquals(Kind.REMIND, step(m, Severity.CRITICAL, 65).get(0).kind);
		assertTrue(step(m, Severity.CRITICAL, 70).isEmpty());
		assertEquals(Kind.REMIND, step(m, Severity.CRITICAL, 125).get(0).kind);
		assertEquals(Kind.REMIND, step(m, Severity.CRITICAL, 185).get(0).kind);
		assertTrue(step(m, Severity.CRITICAL, 245).isEmpty()); // 최대 3회
	}

	@Test
	public void 확인된_경보는_재알림하지_않는다() {
		AlertStateMachine m = new AlertStateMachine();
		step(m, Severity.CRITICAL, 0);
		step(m, Severity.CRITICAL, 5);
		m.markMailSent("disk", 5 * MIN);
		m.ack("disk");
		assertTrue(step(m, Severity.CRITICAL, 65).isEmpty());
	}

	@Test
	public void 메일이_안_나간_경보는_재알림_대상이_아니다() {
		AlertStateMachine m = new AlertStateMachine();
		step(m, Severity.CRITICAL, 0);
		step(m, Severity.CRITICAL, 5);
		assertTrue(step(m, Severity.CRITICAL, 65).isEmpty());
	}

	@Test
	public void 위험은_재알림하지_않는다_긴급만() {
		AlertStateMachine m = new AlertStateMachine();
		step(m, Severity.DANGER, 0);
		step(m, Severity.DANGER, 5);
		m.markMailSent("disk", 5 * MIN);
		assertTrue(step(m, Severity.DANGER, 65).isEmpty());
	}

	@Test
	public void 재기동_복원_뒤에는_같은_등급_관측에_OPEN_이_다시_나지_않는다() {
		AlertStateMachine m = new AlertStateMachine();
		m.restore("disk", Severity.DANGER, 0, 12L, false, true, 1);
		assertTrue(step(m, Severity.DANGER, 5).isEmpty());
		assertTrue(step(m, Severity.DANGER, 10).isEmpty());
		assertEquals(12L, m.state("disk").alertId);
		// 복구는 정상 동작
		step(m, Severity.NORMAL, 15);
		List<Transition> t = step(m, Severity.NORMAL, 20);
		assertEquals(Kind.RESOLVE, t.get(0).kind);
		assertEquals(12L, t.get(0).alertId);
	}

	@Test
	public void 항목은_서로_독립이다() {
		AlertStateMachine m = new AlertStateMachine();
		m.apply("disk", Severity.DANGER, 90, "", 0, 2, 2, REMIND, 3);
		m.apply("cpu", Severity.NORMAL, 10, "", 0, 2, 2, REMIND, 3);
		List<Transition> t = m.apply("disk", Severity.DANGER, 90, "", 5 * MIN, 2, 2, REMIND, 3);
		assertEquals(Kind.OPEN, t.get(0).kind);
		assertEquals(Severity.NORMAL, m.state("cpu").open);
		assertEquals(2, m.states().size());
	}

	@Test
	public void confirmCount_1이면_즉시_연다() {
		AlertStateMachine m = new AlertStateMachine();
		List<Transition> t = m.apply("db", Severity.CRITICAL, 0, "down", 0, 1, 1, REMIND, 3);
		assertEquals(Kind.OPEN, t.get(0).kind);
		assertEquals(Kind.RESOLVE, m.apply("db", Severity.NORMAL, 1, "up", 5 * MIN, 1, 1, REMIND, 3).get(0).kind);
	}

	// ===== 2-35차: 열린 등급보다 낮아지면 복구 =====

	@Test
	public void 위험_경보가_주의로_두_번_내려오면_복구되고_주의_경보가_이어서_열린다() {
		AlertStateMachine m = new AlertStateMachine();
		step(m, Severity.DANGER, 0);
		step(m, Severity.DANGER, 5);
		m.bindAlertId("disk", 9L);
		m.markMailSent("disk", 5 * MIN);
		assertTrue(step(m, Severity.WARNING, 10).isEmpty());
		List<Transition> t = step(m, Severity.WARNING, 15);
		assertEquals(1, t.size());
		assertEquals(Kind.RESOLVE, t.get(0).kind);
		assertEquals(Severity.DANGER, t.get(0).previousSeverity); // 닫힌 경보의 최고 등급
		assertEquals(Severity.WARNING, t.get(0).severity);        // 복구 시점 현재 등급
		assertEquals(9L, t.get(0).alertId);
		assertTrue(t.get(0).mailSent);
		// 주의가 계속되면 다음 관측에서 주의 경보가 새로 열린다 (복구 시점에 1회 센 것으로 본다)
		List<Transition> next = step(m, Severity.WARNING, 20);
		assertEquals(Kind.OPEN, next.get(0).kind);
		assertEquals(Severity.WARNING, next.get(0).severity);
	}

	@Test
	public void 낮아졌다가_다시_원래_등급이면_복구_카운트가_리셋된다() {
		AlertStateMachine m = new AlertStateMachine();
		step(m, Severity.CRITICAL, 0);
		step(m, Severity.CRITICAL, 5);
		step(m, Severity.DANGER, 10);
		step(m, Severity.CRITICAL, 15);
		assertTrue(step(m, Severity.DANGER, 20).isEmpty());
		assertEquals(Severity.CRITICAL, m.state("disk").open);
	}
}

