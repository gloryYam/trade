package trade.tradestream.market.infra;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import trade.tradestream.market.domain.PriceTick;

import java.time.Instant;

/**
 * Binance WebSocket 메시지 핸들러
 *
 * ============================================================
 * [TextWebSocketHandler란?]
 * ============================================================
 * Spring WebSocket이 제공하는 추상 클래스
 * 텍스트 기반 WebSocket 메시지를 처리할 때 상속받아 사용
 *
 * 주요 메서드:
 * - afterConnectionEstablished() : 연결 성공 시 호출
 * - handleTextMessage()          : 메시지 수신 시 호출
 * - handleTransportError()       : 에러 발생 시 호출
 * - afterConnectionClosed()      : 연결 종료 시 호출
 *
 * ============================================================
 * [데이터 흐름]
 * ============================================================
 * 1. Binance에서 JSON 메시지 수신
 *    {"s":"BTCUSDT","b":"42000.00","a":"42001.00",...}
 *
 * 2. ObjectMapper로 BinanceBookTickerMessage로 변환
 *
 * 3. PriceTick 도메인 객체로 변환
 *    (외부 API 형식 → 우리 도메인 형식)
 *
 * 4. LatestPriceStore에 저장 (Redis 캐시)
 *
 * ============================================================
 * [Walking Skeleton - 최소 구현]
 * ============================================================
 * - 메시지 수신 → 파싱 → 저장 기본 흐름만
 * - 에러 발생 시 로그만 출력 (예외 던지지 않음)
 * - 재연결 로직 없음 → Step 2에서 추가
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final LatestPriceStore latestPriceStore;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Binance WebSocket 연결 성공: {}", session.getId());
    }


    /**
     * 메시지 수신 시 호출 (핵심 로직)
     *
     * [처리 흐름]
     * 1. JSON 문자열 → BinanceBookTickerMessage 파싱
     * 2. BinanceBookTickerMessage → PriceTick 변환
     * 3. Redis에 저장
     *
     * @param session WebSocket 세션
     * @param message 수신된 텍스트 메시지 (JSON)
     */

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("메세지 수신 : {}", payload);

        try {

            // 1. JSON → DTO 파싱
            BinanceBookTickerMessage tickerMessage = objectMapper.readValue(payload, BinanceBookTickerMessage.class);

            // 2. DTO → 도메인 객체 변환
            PriceTick priceTick = new PriceTick(
                    tickerMessage.symbol(),
                    tickerMessage.bidPrice(),
                    tickerMessage.askPrice(),
                    Instant.now()
            );

            // 3. Redis에 저장
            latestPriceStore.save(priceTick);

            log.info("가격 업데이트: {} bid={} ask={}", priceTick.symbol(), priceTick.bidPrice(), priceTick.askPrice());


        } catch (Exception e) {
            // Walking Skeleton: 에러 시 로그만 출력
            // 하나의 메시지 실패가 전체 연결에 영향 주지 않도록
            log.error("메시지 처리 실패: {}", payload, e);
        }

    }

    /**
     * 전송 에러 발생 시 호출
     *
     * @param session   WebSocket 세션
     * @param exception 발생한 예외
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 전송 에러: {}", session.getId(), exception);
    }


    /**
     * 연결 종료 시 호출
     *
     * @param session WebSocket 세션
     * @param status  종료 상태 (정상 종료인지, 에러로 종료인지 등)
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("Binance WebSocket 연결 종료: {} (status={})", session.getId(), status);
        // TODO: Step 2에서 재연결 로직 추가
    }

}
