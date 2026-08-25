package com.sensible.common.filter;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.sensible.common.util.RequestMetrics;

/**
 * 요청 수와 응답 바이트를 세는 필터.
 *
 * 목적은 "어떤 경로가 트래픽을 얼마나 먹는지" 분해하는 것이다.
 * 과금 기준이 되는 실제 아웃바운드 총량은 NIC 카운터(/proc/net/dev)로 따로 읽는다.
 *
 * 안전 원칙:
 * - 통계 수집이 실패해도 요청 처리는 그대로 진행한다.
 * - getWriter()는 컨테이너 것을 그대로 넘긴다. JSP 렌더링 경로를 건드리지 않기 위함이며,
 *   이 경우 응답 바이트는 setContentLength 값으로 대체한다.
 * - 바이트 측정은 getOutputStream() 경로에서만 한다. API JSON·이미지·미디어 등
 *   트래픽 대부분이 이 경로를 쓴다.
 *
 * init-param enabled=false 로 즉시 무력화할 수 있다.
 */
public class MetricsCounterFilter implements Filter {

    private boolean enabled = true;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String v = filterConfig.getInitParameter("enabled");
        if (v != null && "false".equalsIgnoreCase(v.trim())) {
            enabled = false;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled || !(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        CountingResponseWrapper wrapper;
        try {
            wrapper = new CountingResponseWrapper((HttpServletResponse) response);
        } catch (Throwable t) {
            // 래핑 실패 시 통계를 포기하고 원본으로 진행
            chain.doFilter(request, response);
            return;
        }

        try {
            chain.doFilter(request, wrapper);
        } finally {
            try {
                String uri = req.getRequestURI();
                String ctx = req.getContextPath();
                if (ctx != null && !ctx.isEmpty() && uri != null && uri.startsWith(ctx)) {
                    uri = uri.substring(ctx.length());
                }
                RequestMetrics.record(RequestMetrics.normalizePath(uri),
                        wrapper.getCapturedStatus(), wrapper.getByteCount());
            } catch (Throwable ignore) {
                // 통계 실패는 무시
            }
        }
    }

    @Override
    public void destroy() {
    }

    /**
     * 상태코드와 출력 바이트를 잡아내는 응답 래퍼.
     * servlet-api 2.5로 컴파일되므로 getStatus()를 쓸 수 없어 직접 기록한다.
     */
    static class CountingResponseWrapper extends HttpServletResponseWrapper {

        private int capturedStatus = HttpServletResponse.SC_OK;
        private long declaredContentLength = -1;
        private CountingServletOutputStream countingStream;

        CountingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            capturedStatus = sc;
            super.setStatus(sc);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void setStatus(int sc, String sm) {
            capturedStatus = sc;
            super.setStatus(sc, sm);
        }

        @Override
        public void sendError(int sc) throws IOException {
            capturedStatus = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            capturedStatus = sc;
            super.sendError(sc, msg);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            capturedStatus = HttpServletResponse.SC_FOUND;
            super.sendRedirect(location);
        }

        @Override
        public void setContentLength(int len) {
            declaredContentLength = len;
            super.setContentLength(len);
        }

        @Override
        public void setHeader(String name, String value) {
            captureContentLength(name, value);
            super.setHeader(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            captureContentLength(name, value);
            super.addHeader(name, value);
        }

        private void captureContentLength(String name, String value) {
            if (name != null && "Content-Length".equalsIgnoreCase(name) && value != null) {
                try {
                    declaredContentLength = Long.parseLong(value.trim());
                } catch (NumberFormatException ignore) {
                    // 파싱 실패는 무시
                }
            }
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (countingStream == null) {
                countingStream = new CountingServletOutputStream(super.getOutputStream());
            }
            return countingStream;
        }

        int getCapturedStatus() {
            return capturedStatus;
        }

        /**
         * 실제 기록된 응답 바이트.
         * OutputStream을 쓴 응답은 실측값, Writer만 쓴 응답(JSP 등)은 Content-Length 값으로 대체한다.
         */
        long getByteCount() {
            if (countingStream != null) {
                return countingStream.getCount();
            }
            return (declaredContentLength > 0) ? declaredContentLength : 0L;
        }
    }

    /** 쓰기 바이트를 세면서 원본 스트림에 그대로 위임 */
    static class CountingServletOutputStream extends ServletOutputStream {

        private final OutputStream delegate;
        private long count;

        CountingServletOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
            count++;
        }

        @Override
        public void write(byte[] b) throws IOException {
            delegate.write(b);
            count += b.length;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            count += len;
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        long getCount() {
            return count;
        }
    }
}
