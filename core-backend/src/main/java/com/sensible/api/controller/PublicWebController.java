package com.sensible.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.sensible.api.service.ContactService;
import com.sensible.api.service.PublicWebFaq;
import com.sensible.api.service.SuperAppService;
import com.sensible.common.util.WebCardUtil;

/**
 * 공개 웹사이트 전용 컨트롤러 (/about, /faq, /contact, /posts, 공개 404).
 *
 * 루트 허브(/)와 콘텐츠 랜딩(/post, /star)은 DeepLinkController 에 그대로 둔다 —
 * 그쪽은 딥링크·OG 조립과 얽혀 있어 떼어내면 회귀 위험이 크다. 새로 만드는
 * 독립 페이지만 여기 모은다.
 */
@Controller
public class PublicWebController {

	@Resource(name = "superAppService")
	private SuperAppService superAppService;

	@Resource(name = "contactService")
	private ContactService contactService;

	/** /posts 한 페이지에 싣는 글 수. sitemap 의 목록 페이지 수 계산도 이 값을 본다 */
	static final int POSTS_PER_PAGE = 24;

	// ─────────────────────────────────────────────────────────
	// 서비스 소개
	// ─────────────────────────────────────────────────────────
	@RequestMapping(value = "/about", method = RequestMethod.GET)
	public String about(HttpServletRequest request, Model model) {
		model.addAttribute("activeNav", "about");
		model.addAttribute("canonicalUrl", WebCardUtil.getBaseUrl(request) + "/about");
		return "/common/about";
	}

	// ─────────────────────────────────────────────────────────
	// FAQ
	// ─────────────────────────────────────────────────────────
	@RequestMapping(value = "/faq", method = RequestMethod.GET)
	public String faq(HttpServletRequest request, Model model) {
		model.addAttribute("activeNav", "faq");
		model.addAttribute("canonicalUrl", WebCardUtil.getBaseUrl(request) + "/faq");
		// 화면과 구조화 데이터가 같은 원본을 본다 (PublicWebFaq)
		model.addAttribute("faqSections", PublicWebFaq.sections());
		model.addAttribute("faqJsonLd", PublicWebFaq.toJsonLd(PublicWebFaq.sections()));
		return "/common/faq";
	}

	// ─────────────────────────────────────────────────────────
	// 공개 포스트 목록
	// ─────────────────────────────────────────────────────────
	@RequestMapping(value = "/posts", method = RequestMethod.GET)
	public String posts(@RequestParam(value = "page", required = false) String pageParam,
			HttpServletRequest request, HttpServletResponse response, Model model) {

		String baseUrl = WebCardUtil.getBaseUrl(request);
		int page = parsePage(pageParam);
		int total = superAppService.getPublicPostCount();
		int lastPage = total <= 0 ? 1 : ((total - 1) / POSTS_PER_PAGE) + 1;

		// 범위 밖 페이지는 빈 목록을 200으로 주지 않는다. 내용 없는 페이지가 색인되면
		// 그 자체가 저품질 페이지로 잡힌다
		if (page > lastPage) {
			return notFound(response, model, baseUrl);
		}

		List<Map<String, Object>> posts = superAppService.getPublicPosts((page - 1) * POSTS_PER_PAGE, POSTS_PER_PAGE);

		model.addAttribute("activeNav", "posts");
		model.addAttribute("postCards", WebCardUtil.toPostCards(posts, baseUrl));
		model.addAttribute("page", page);
		model.addAttribute("lastPage", lastPage);
		model.addAttribute("totalCount", total);
		// 페이지마다 canonical 이 달라야 2페이지 이후가 1페이지의 중복으로 취급되지 않는다
		model.addAttribute("canonicalUrl", pageUrl(baseUrl, page));
		model.addAttribute("prevUrl", page > 1 ? pageUrl(baseUrl, page - 1) : null);
		model.addAttribute("nextUrl", page < lastPage ? pageUrl(baseUrl, page + 1) : null);
		return "/common/posts";
	}

	// ─────────────────────────────────────────────────────────
	// 문의하기
	// ─────────────────────────────────────────────────────────
	@RequestMapping(value = "/contact", method = RequestMethod.GET)
	public String contactForm(@RequestParam(value = "type", required = false) String type,
			HttpServletRequest request, Model model) {
		prepareContactForm(request, model);
		// 푸터의 "Account Help"·"Partnership" 링크가 유형을 미리 골라준다
		if (ContactService.isKnownType(type)) {
			model.addAttribute("selectedType", type);
		}
		return "/common/contact";
	}

	@RequestMapping(value = "/contact", method = RequestMethod.POST)
	public String contactSubmit(HttpServletRequest request, Model model) {
		Map<String, String> form = new HashMap<>();
		form.put("contactType", request.getParameter("contactType"));
		form.put("senderName", request.getParameter("senderName"));
		form.put("senderEmail", request.getParameter("senderEmail"));
		form.put("subject", request.getParameter("subject"));
		form.put("message", request.getParameter("message"));
		form.put("privacyAgree", request.getParameter("privacyAgree"));
		form.put("formTime", request.getParameter("formTime"));
		form.put("website", request.getParameter("website"));

		Map<String, Object> result = contactService.submit(form, request);

		prepareContactForm(request, model);

		if ("OK".equals(result.get("result"))) {
			model.addAttribute("submitted", true);
		} else {
			model.addAttribute("errorMsg", result.get("msg"));
			model.addAttribute("errorField", result.get("field"));
			// 실패했을 때 입력값을 날리면 길게 쓴 사람이 처음부터 다시 써야 한다
			model.addAttribute("selectedType", form.get("contactType"));
			model.addAttribute("inputName", form.get("senderName"));
			model.addAttribute("inputEmail", form.get("senderEmail"));
			model.addAttribute("inputSubject", form.get("subject"));
			model.addAttribute("inputMessage", form.get("message"));
			model.addAttribute("inputAgree", form.get("privacyAgree"));
		}
		return "/common/contact";
	}

	private void prepareContactForm(HttpServletRequest request, Model model) {
		model.addAttribute("activeNav", "contact");
		model.addAttribute("canonicalUrl", WebCardUtil.getBaseUrl(request) + "/contact");
		model.addAttribute("contactTypes", ContactService.types());
		// 최소 작성 시간 검증용. 폼을 그린 시각을 hidden 으로 들려 보낸다
		model.addAttribute("formTime", System.currentTimeMillis());
	}

	// ─────────────────────────────────────────────────────────
	// 공개 404
	// ─────────────────────────────────────────────────────────
	/**
	 * web.xml 의 404 error-page 가 여기로 들어온다.
	 *
	 * 관리자 화면(/adm, /super, /login)의 404 는 기존 어드민 에러 페이지를 그대로 쓴다.
	 * 관리자 레이아웃 안에서 공개 사이트 헤더·푸터가 뜨는 게 더 혼란스럽다.
	 */
	@RequestMapping(value = "/error/not-found")
	public String notFoundPage(HttpServletRequest request, HttpServletResponse response, Model model) {
		Object originalUri = request.getAttribute("javax.servlet.error.request_uri");
		String uri = originalUri == null ? "" : String.valueOf(originalUri);
		String contextPath = request.getContextPath();
		if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
			uri = uri.substring(contextPath.length());
		}

		if (uri.startsWith("/adm/") || uri.startsWith("/super/") || uri.startsWith("/login/")) {
			return "error/404Error.main";
		}

		return notFound(response, model, WebCardUtil.getBaseUrl(request));
	}

	/**
	 * 공개 404 렌더링. 상태 코드를 직접 404 로 세운다 —
	 * 컨트롤러가 정상 뷰를 돌려주면 컨테이너는 200 으로 응답하고, 그것이 곧 Soft 404 다.
	 */
	private String notFound(HttpServletResponse response, Model model, String baseUrl) {
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		model.addAttribute("baseUrl", baseUrl);
		return "/common/web_404";
	}

	private String pageUrl(String baseUrl, int page) {
		return page <= 1 ? baseUrl + "/posts" : baseUrl + "/posts?page=" + page;
	}

	/** 잘못된 page 파라미터는 1페이지로 본다 (문자·0·음수 모두) */
	private int parsePage(String raw) {
		if (raw == null) {
			return 1;
		}
		try {
			int page = Integer.parseInt(raw.trim());
			return page < 1 ? 1 : page;
		} catch (NumberFormatException e) {
			return 1;
		}
	}
}
