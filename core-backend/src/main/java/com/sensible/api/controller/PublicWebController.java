package com.sensible.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.sensible.api.service.ContactService;
import com.sensible.api.service.PublicWebFaq;
import com.sensible.api.service.SuperAppService;
import com.sensible.common.util.WebCardUtil;

/**
 * 공개 웹사이트 전용 컨트롤러 (/, /posts, /about, /faq, /contact, 공개 404).
 *
 * [2-29차 후속] Home 과 Posts 의 내용을 맞바꿨다 (클라이언트 확정).
 *   - /       → 공개 포스트 목록 (home.jsp). 페이지네이션은 /?page=N
 *   - /posts  → 예전 루트 허브였던 마케팅 페이지 (posts.jsp: 히어로·인기 스타·FAQ·앱 CTA)
 *   - /posts?page=N → 이미 색인된 옛 목록 URL. /?page=N 으로 301 영구 이동
 * 루트 허브가 DeepLinkController 에 있던 이유(딥링크·OG 와의 결합)는 실제로는 없었고
 * 이번 교차 작업으로 두 화면이 서로 얽히게 되어 여기로 옮겼다. 콘텐츠 랜딩(/post, /star)과
 * sitemap 은 여전히 DeepLinkController 에 있다.
 */
@Controller
public class PublicWebController {

	@Resource(name = "superAppService")
	private SuperAppService superAppService;

	@Resource(name = "contactService")
	private ContactService contactService;

	/** 홈(/) 목록 한 페이지에 싣는 글 수. sitemap 의 목록 페이지 수 계산도 이 값을 본다 */
	static final int POSTS_PER_PAGE = 24;

	// ─────────────────────────────────────────────────────────
	// 홈: 공개 포스트 목록
	// ─────────────────────────────────────────────────────────
	/**
	 * 루트(/). 서브 라우트 없이 진입한 전원(크롤러 포함)에게 공개 포스트 목록을 서버 렌더링한다.
	 * UA 별 분기는 클로킹 오해 소지가 있어 두지 않고, 앱 전환은 Open in App 버튼 클릭으로만 시도한다.
	 */
	@RequestMapping(value = "/")
	public String home(@RequestParam(value = "page", required = false) String pageParam,
			HttpServletRequest request, HttpServletResponse response, Model model) {

		String baseUrl = WebCardUtil.getBaseUrl(request);
		int page = parsePage(pageParam);
		int total = superAppService.getPublicPostCount();
		int lastPage = lastPage(total);

		// 범위 밖 페이지는 빈 목록을 200으로 주지 않는다. 내용 없는 페이지가 색인되면
		// 그 자체가 저품질 페이지로 잡힌다
		if (page > lastPage) {
			return notFound(response, model, baseUrl);
		}

		List<Map<String, Object>> posts = superAppService.getPublicPosts((page - 1) * POSTS_PER_PAGE, POSTS_PER_PAGE);

		model.addAttribute("activeNav", "home");
		model.addAttribute("postCards", WebCardUtil.toPostCards(posts, baseUrl));
		model.addAttribute("page", page);
		model.addAttribute("lastPage", lastPage);
		model.addAttribute("totalCount", total);
		// 페이지마다 canonical 이 달라야 2페이지 이후가 1페이지의 중복으로 취급되지 않는다
		model.addAttribute("canonicalUrl", pageUrl(baseUrl, page));
		model.addAttribute("prevUrl", page > 1 ? pageUrl(baseUrl, page - 1) : null);
		model.addAttribute("nextUrl", page < lastPage ? pageUrl(baseUrl, page + 1) : null);
		return "/common/home";
	}

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
	// Posts: 서비스 소개 허브 (예전 루트 허브)
	// ─────────────────────────────────────────────────────────
	/**
	 * /posts. 히어로·최근 포스트·인기 스타·Why·How·FAQ·앱 CTA 로 이루어진 마케팅 페이지.
	 * FAQPage JSON-LD 도 이 화면(posts.jsp)에 함께 있다.
	 *
	 * page 파라미터가 붙어 있으면 옛 목록 URL(/posts?page=N)이다 — 이미 검색엔진에 색인돼 있으므로
	 * 404 나 다른 내용을 주지 않고 새 목록 주소(/?page=N)로 301 영구 이동시킨다.
	 */
	@RequestMapping(value = "/posts", method = RequestMethod.GET)
	public ModelAndView posts(@RequestParam(value = "page", required = false) String pageParam,
			HttpServletRequest request) {

		String baseUrl = WebCardUtil.getBaseUrl(request);

		if (pageParam != null) {
			RedirectView redirect = new RedirectView(legacyPostsRedirectUrl(baseUrl, pageParam), false);
			redirect.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
			// 모델 속성이 쿼리스트링으로 따라붙지 않게 한다
			redirect.setExposeModelAttributes(false);
			return new ModelAndView(redirect);
		}

		ModelAndView mav = new ModelAndView("/common/posts");
		mav.addObject("canonicalUrl", baseUrl + "/posts");
		mav.addObject("activeNav", "posts");

		try {
			// 최근 포스트 카드: 크롤러가 광고·콘텐츠가 있는 /post/*를 발견하는 내부 링크 경로
			List<Map<String, Object>> posts = superAppService.getHomeRecentPosts();
			// 카드 가공은 홈 목록과 공유한다 (WebCardUtil)
			mav.addObject("recentPosts", WebCardUtil.toPostCards(posts, baseUrl));

			// 인기 스타 카드: /star/* 내부 링크 경로
			List<Map<String, Object>> stars = superAppService.getHomeTopStars();
			List<Map<String, Object>> starCards = new java.util.ArrayList<>();
			for (Map<String, Object> star : stars) {
				Map<String, Object> card = new HashMap<>();
				card.put("id", star.get("PRS_ID"));
				card.put("name", WebCardUtil.escapeHtml(String.valueOf(star.get("PRS_NAME"))));
				card.put("image", WebCardUtil.toAbsoluteUrl((String) star.get("STORED_FILE_NM"), baseUrl));
				card.put("followerCnt", star.get("FOLLOWER_CNT"));
				starCards.add(card);
			}
			mav.addObject("topStars", starCards);
		} catch (Exception e) {
			// 목록 조회가 실패해도 허브 골격(히어로·소개·푸터)은 렌더링한다
			e.printStackTrace();
		}

		return mav;
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

	/** 전체 글 수로 목록 마지막 페이지 번호를 구한다. 글이 없어도 1페이지는 있다 */
	static int lastPage(int total) {
		return total <= 0 ? 1 : ((total - 1) / POSTS_PER_PAGE) + 1;
	}

	/**
	 * 홈 목록 페이지 URL. 1페이지는 루트 그대로다 —
	 * / 와 /?page=1 이 각각 색인되면 같은 내용이 중복 URL 로 잡힌다.
	 * sitemap(DeepLinkController)도 이 규칙으로 목록 URL 을 나열한다.
	 */
	static String pageUrl(String baseUrl, int page) {
		return page <= 1 ? baseUrl + "/" : baseUrl + "/?page=" + page;
	}

	/** 옛 목록 URL(/posts?page=N)의 301 이동 대상. 잘못된 page 값은 1페이지(루트)로 보낸다 */
	static String legacyPostsRedirectUrl(String baseUrl, String pageParam) {
		return pageUrl(baseUrl, parsePage(pageParam));
	}

	/** 잘못된 page 파라미터는 1페이지로 본다 (문자·0·음수 모두) */
	static int parsePage(String raw) {
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
