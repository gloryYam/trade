package trade.tradestream.market.infra;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Binance WebSocket 클라이언트 (연결 관리)
 *
 * ============================================================
 * [이 클래스의 역할]
 * ============================================================
 * - Binance WebSocket 서버에 연결
 * - 여러 심볼 구독 관리 (BTCUSDT, ETHUSDT 등)
 * - 애플리케이션 시작 시 자동 연결
 * - 애플리케이션 종료 시 정리(cleanup)
 *
 * ============================================================
 * [Binance WebSocket URL 형식]
 * ============================================================
 * 단일 스트림:
 *   wss://stream.binance.com:9443/ws/btcusdt@bookTicker
 *
 * 복합 스트림 (여러 심볼 한 번에):
 *   wss://stream.binance.com:9443/stream?streams=btcusdt@bookTicker/ethusdt@bookTicker
 *
 * 이 구현에서는 단일 스트림 방식 사용 (심볼당 하나의 연결)
 * → 간단하고 디버깅 쉬움
 * → Step 2에서 복합 스트림으로 최적화 가능
 *
 * ============================================================
 * [StandardWebSocketClient란?]
 * ============================================================
 * Spring이 제공하는 WebSocket 클라이언트
 * Java 표준 WebSocket API (JSR-356) 기반
 * 별도 라이브러리 없이 사용 가능
 *
 * ============================================================
 * [@PostConstruct / @PreDestroy]
 * ============================================================
 * @PostConstruct: Bean 생성 + 의존성 주입 완료 후 호출
 *                 → 애플리케이션 시작 시 WebSocket 연결
 *
 * @PreDestroy: Bean 소멸 전 호출
 *              → 애플리케이션 종료 시 연결 정리
 *
 * ============================================================
 * [Walking Skeleton - 최소 구현]
 * ============================================================
 * - 하드코딩된 심볼 목록 (BTCUSDT, ETHUSDT)
 * - 단순 연결만 (재연결 없음)
 * - 연결 실패 시 로그만 출력
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceWebSocketClient {

    // Binance WebSocket 기본 URL
    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/";

    // 구독할 심볼 목록 (Walking Skeleton: 하드코딩)
    // TODO: application.yml에서 설정으로 빼기
    private static final List<String> SYMBOLS = List.of("btcusdt", "ethusdt");

    private final BinanceWebSocketHandler webSocketHandler;

    // 활성 세션 관리 (스레드 안전한 리스트)
    // CopyOnWriteArrayList: 읽기 많고 쓰기 적을 때 적합
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();


    /**
     * 애플리케이션 시작 시 WebSocket 연결
     *
     * [실행 시점]
     * Spring 컨테이너가 이 Bean을 생성하고
     * 모든 의존성 주입이 완료된 직후 자동 호출
     */
    @PostConstruct
    public void connect() {
        log.info(("Binance WebSocket 연결 시작..."));

        // StandardWebSocketClient 생성
        StandardWebSocketClient client = new StandardWebSocketClient();

        for (String symbol : SYMBOLS) {
            connectToSymbol(client, symbol);
        }

        log.info("Binance WebSocket 연결 완료. 구독 심볼: {}", SYMBOLS);
    }

    private void connectToSymbol(StandardWebSocketClient client, String symbol) {
        // bookTicker 스트림 URL 생성
        String url = BINANCE_WS_URL + symbol + "@bookTicker";

        // 비동기로 연결 시도
        // .get()으로 연결 완료까지 대기 (동기화)
        try {
            WebSocketSession session = client.execute(webSocketHandler, url).get();

            // 연결된 세션 저장
            sessions.add(session);
            log.info("심볼 {} 연결 성공", symbol.toUpperCase());
        } catch (Exception e) {

            log.error("심볼 {} 연결 실패: {}", symbol.toUpperCase(), e.getMessage());
        }
    }

    /**
     * 애플리케이션 종료 시 모든 연결 정리
     *
     * [왜 필요한가?]
     * WebSocket 연결은 리소스를 점유함
     * 명시적으로 닫지 않으면 리소스 누수 발생 가능
     */
    @PreDestroy
    public void disconnect() {
        log.info("Binance WebSocket 연결 종료 중...");

        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (Exception e) {
                log.error("세션 종료 실패: {}", session.getId(), e);
            }
        }

        sessions.clear();
        log.info("Binance WebSocket 모든 연결 종료 완료");
    }

    /**
     * 현재 연결된 세션 수 반환 (모니터링용)
     */
    public int getActiveSessionCount() {
        return (int) sessions.stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }

}
