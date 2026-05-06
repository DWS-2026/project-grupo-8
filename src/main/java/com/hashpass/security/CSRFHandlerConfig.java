package com.hashpass.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CSRFHandlerConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new CSRFHandlerInterceptor());
		registry.addInterceptor(rateLimitInterceptor);
	}

	private final RateLimitInterceptor rateLimitInterceptor;

	public CSRFHandlerConfig(RateLimitInterceptor rateLimitInterceptor) {
		this.rateLimitInterceptor = rateLimitInterceptor;
	}
}

class CSRFHandlerInterceptor implements HandlerInterceptor {

	@Override
	public void postHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler,
			final ModelAndView modelAndView) throws Exception {

		if (modelAndView != null) {

			CsrfToken token = (CsrfToken) request.getAttribute("_csrf");
			if (token != null) {
				modelAndView.addObject("token", token.getToken());
				// Expose header and parameter names so client-side JS can add the CSRF header
				modelAndView.addObject("csrfHeaderName", token.getHeaderName());
				modelAndView.addObject("csrfParameterName", token.getParameterName());
			}
		}
	}
}