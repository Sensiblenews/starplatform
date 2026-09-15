package com.sensible.api.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 공개 웹 FAQ(/faq)의 문항과 답변.
 *
 * 화면 텍스트와 FAQPage 구조화 데이터가 어긋나면 구조화 데이터 스팸으로 판정될 수 있다.
 * 2-27차 루트 허브에서는 JSP 본문과 JSON-LD 블록을 각각 손으로 유지했는데,
 * 한쪽만 고치면 곧바로 어긋나는 구조였다. 여기서는 문항을 이 클래스 한 곳에만 두고
 * 화면과 JSON-LD를 모두 여기서 만들어 어긋날 수가 없게 한다.
 *
 * 답변은 실제 동작만 적는다. 확인되지 않은 기능은 문항 자체를 만들지 않는다.
 */
public final class PublicWebFaq {

	private PublicWebFaq() {
	}

	/** FAQ 한 문항. JSP EL에서 읽으므로 getter가 필요하다 */
	public static final class Item {
		private final String id;
		private final String question;
		private final String answer;

		public Item(String id, String question, String answer) {
			this.id = id;
			this.question = question;
			this.answer = answer;
		}

		public String getId() {
			return id;
		}

		public String getQuestion() {
			return question;
		}

		public String getAnswer() {
			return answer;
		}
	}

	/** 문항 묶음. id는 목차·푸터에서 앵커(#id)로 쓴다 — 바꾸면 기존 링크가 깨진다 */
	public static final class Section {
		private final String id;
		private final String title;
		private final List<Item> items;

		public Section(String id, String title, Item... items) {
			this.id = id;
			this.title = title;
			this.items = Collections.unmodifiableList(Arrays.asList(items));
		}

		public String getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public List<Item> getItems() {
			return items;
		}
	}

	private static final List<Section> SECTIONS = Collections.unmodifiableList(Arrays.asList(
		new Section("general", "About the service",
			new Item(null, "What is StarPlatform?",
				"StarPlatform is a global creator platform. Every member runs a public page, publishes posts to it, and competes in a single worldwide ranking built from real engagement. The traffic a page attracts also determines what its owner earns from automated advertising. Reading is open to everyone, and publishing requires the mobile app."),
			new Item(null, "How is StarPlatform different from Facebook, Instagram, or YouTube?",
				"Those services rank what you see against the people you already follow. StarPlatform ranks every page against every other page on the service, in one public list that crosses countries and categories. The inputs to that ranking — visits, likes, bookmarks, followers — are visible rather than hidden inside a recommendation engine. StarPlatform is also not a replacement for the networks you already use: posts carry share links that work anywhere."),
			new Item(null, "Do I need an account to read content on this website?",
				"No. Public star pages and public posts are readable in full on this website without an account, without the app, and without accepting a login prompt. An account is needed only to publish, follow, comment, or send messages."),
			new Item(null, "Is StarPlatform free?",
				"Yes. Creating a page, publishing posts, following other pages, and reading content are all free. There is no subscription tier, and the service is funded by the advertising shown alongside content.")),

		new Section("star-pages", "Star pages",
			new Item(null, "What is a star page?",
				"A star page is your page on StarPlatform. It holds your profile, your posts, your follower and visitor counts, and your current global rank. It has a permanent public address that you can share anywhere, and anyone who opens that link sees the page itself rather than an install prompt."),
			new Item(null, "Can anyone create a star page?",
				"Yes. Install the StarPlatform app, sign up, and create your page. Individuals, creators, brands, organizations, universities, cities, and media outlets all use the same page format, and the page type is shown on the page so visitors know who they are reading."),
			new Item(null, "Is my page address permanent?",
				"Yes. Each star page keeps the same public web address for as long as the page exists, so links you have already shared keep working. Changing your display name or profile photo does not change the address."),
			new Item(null, "Can I change my page name, photo, or introduction?",
				"Your display name and profile photo can be changed in the app at any time. The page introduction shown on the web version of your page is currently written by the operations team — contact us if you would like yours added or updated."),
			new Item(null, "Can I share my star page on other social networks?",
				"Yes, and it helps your ranking. Share links are built for use off the platform: they render a preview card on the major networks and messengers, and the visits they bring back count toward your rank the same as any other visit.")),

		new Section("ranking", "Global ranking",
			new Item(null, "What is the global ranking?",
				"It is a single standing that includes every star page on the service. Position is calculated from real engagement — page visits, likes, bookmarks, and followers — and it moves continuously as those numbers change. A TOP 100 view shows the highest-ranked pages at any moment."),
			new Item(null, "How is Daily King decided?",
				"Daily King is the page that gathered the most engagement over the preceding 24 hours, and it resets every day. Because it measures a single day rather than a lifetime total, a page that is not yet large can still take the top spot on a good day."),
			new Item(null, "What is a VS card?",
				"A VS card places two star pages side by side as a head-to-head matchup, and visitors decide the outcome. It gives each page exposure to the other's audience and gives visitors a way to discover pages they would not have searched for."),
			new Item(null, "How do visits, likes, and bookmarks affect my rank?",
				"They are the inputs the ranking is calculated from, so the ranking responds to genuine attention rather than posting volume. Traffic you bring from outside StarPlatform counts, which is why sharing your page elsewhere moves your position. Traffic that does not pass the platform's validity checks is not counted."),
			new Item(null, "What is the Hall of Fame?",
				"The Hall of Fame is a permanent record of pages that have held a leading position. Where the main ranking shows where things stand now, the Hall of Fame keeps what has already been achieved.")),

		new Section("messages", "Direct messages",
			new Item(null, "Can I message other members?",
				"Yes. Members can start a direct conversation with each other in the app. Messages are private to the two people in the conversation."),
			new Item(null, "When are messages deleted?",
				"Messages are deliberately short-lived. A message is removed about five minutes after it is sent, and about one minute after the recipient reads it, whichever comes first. Attached images are deleted from the server along with the message, so conversations do not build up a permanent archive."),
			new Item(null, "Can I report an inappropriate message?",
				"Yes. Any message can be reported from the conversation. Because messages expire on their own, reporting one preserves a copy of it for the operations team to review — otherwise there would be nothing left to examine by the time the report is read. Accounts found to have broken the rules are suspended."),
			new Item(null, "Can I block someone?",
				"Yes. Blocking a member stops any further messages from reaching you, and you can see and undo your blocks from the block list in the app.")),

		new Section("advertising", "Advertising and earning",
			new Item(null, "Where do ads appear on StarPlatform?",
				"Ads appear in the app alongside content and on the public web pages of this site. They are placed automatically by the platform and by its advertising partners; individual members do not sell or place their own ads."),
			new Item(null, "How does visitor-based earning work?",
				"Advertising revenue is attributed to the page that generated the traffic and accrues to the person who runs it. You do not negotiate rates or manage campaigns — publishing and attracting genuine visitors is the whole of the process. Current earnings and the revenue ranking are visible in the app."),
			new Item(null, "How are earnings calculated?",
				"Earnings follow the advertising a page's traffic actually generates, which depends on how many valid visits and ad impressions it produces. Because advertising rates vary by country, season, and advertiser demand, the same number of visits does not always produce the same amount. Traffic that fails the platform's validity checks is excluded before anything is counted."),
			new Item(null, "Why do I sometimes see no ads?",
				"An ad slot is only filled when a suitable ad is available for that viewer at that moment, so empty slots are normal and vary by country and time of day. Frequency limits also apply in the app so that ads are not shown repeatedly in a short session."),
			new Item(null, "What cookies and tracking does the site use?",
				"Advertising on this site is served by Google AdSense and its partners, which may set cookies to measure and select ads. Where local law requires it, a consent notice is shown before personalised advertising cookies are used. Full details are in our privacy policy.")),

		new Section("app", "Using the app and the web",
			new Item(null, "Can I use StarPlatform without installing the app?",
				"Yes, for reading. Public pages, public posts, this FAQ, and our policy pages all work in a normal web browser on a phone, tablet, or desktop computer. Publishing posts, following, commenting, and messaging require the app."),
			new Item(null, "Is the app available for both iOS and Android?",
				"Yes. StarPlatform is published on the App Store and on Google Play, and the same account works on both."),
			new Item(null, "Can I open a web page directly in the app?",
				"Yes. Public pages carry an Open in App action that hands the page over to the app if you have it installed. If it is not installed, you stay on the web page — you are not redirected to a store against your wishes."),
			new Item(null, "Why do I keep seeing an install prompt?",
				"You should not. Public web pages on this site are meant to show their content to everyone, with the app offered only as an optional action. If a page is blocking content behind an install prompt, please report it to us — that is a bug, not intended behaviour.")),

		new Section("safety", "Reporting, safety, and content policy",
			new Item("content-policy", "What is not allowed on StarPlatform?",
				"Content that is illegal, sexually explicit, hateful, violent, deceptive, or that infringes someone else's rights is not permitted, and neither is impersonating another person or organization. Images uploaded to public pages are reviewed before they appear publicly. Accounts that break these rules have their content removed and may be suspended."),
			new Item(null, "How do I report content?",
				"Posts and messages can be reported from within the app, and anything on this website can be reported through our contact form. Tell us the page or post address and what the problem is; reports are reviewed by the operations team."),
			new Item(null, "How do I report a copyright or rights infringement?",
				"Use the contact form and select the copyright and rights enquiry type. Include the address of the material, a description of the work you hold rights to, and how we can reach you, and we will act on verified reports."),
			new Item(null, "How do I ask about my personal data?",
				"Send a privacy enquiry through the contact form. Our privacy policy describes what is collected and how it is handled, and requests about your own data are handled through the same channel.")),

		new Section("contact", "Contact",
			new Item(null, "How do I contact StarPlatform?",
				"Use the contact form on this site. Choose the enquiry type that fits, and the message reaches the team that handles it."),
			new Item(null, "How do I ask about my account?",
				"Send an account enquiry through the contact form, including the page name or account address involved so we can find it. Please do not send passwords — we never need them."),
			new Item(null, "How do I propose a partnership?",
				"Send a partnership enquiry through the contact form with a short description of what you have in mind and who you represent. Advertising, brand, institutional, and operating partner enquiries all use the same channel."),
			new Item(null, "How long does a reply take?",
				"Enquiries are read in the order they arrive and answered by email. Reports of illegal content or rights infringement are prioritised ahead of general enquiries."))
	));

	/** 화면 렌더링과 JSON-LD가 함께 참조하는 유일한 원본 */
	public static List<Section> sections() {
		return SECTIONS;
	}

	/**
	 * FAQPage 구조화 데이터를 만든다. 화면에 뿌리는 sections()와 같은 데이터를 쓰므로
	 * 문구를 고치면 양쪽이 동시에 따라간다.
	 */
	public static String toJsonLd(List<Section> sections) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"@context\":\"https://schema.org\",\"@type\":\"FAQPage\",\"mainEntity\":[");

		boolean first = true;
		for (Section section : sections) {
			for (Item item : section.getItems()) {
				if (!first) {
					sb.append(',');
				}
				first = false;
				sb.append("{\"@type\":\"Question\",\"name\":\"").append(escapeJson(item.getQuestion()))
					.append("\",\"acceptedAnswer\":{\"@type\":\"Answer\",\"text\":\"")
					.append(escapeJson(item.getAnswer())).append("\"}}");
			}
		}

		sb.append("]}");
		return sb.toString();
	}

	/**
	 * JSON 문자열 이스케이프. '/'를 \/로 바꾸는 것이 핵심이다 — 답변에 우연히 "</script>"가
	 * 들어가면 스크립트 블록이 거기서 끊긴다. (DeepLinkController의 같은 이름 헬퍼와 동일 규칙)
	 */
	static String escapeJson(String s) {
		if (s == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(s.length() + 16);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '/':
					sb.append("\\/");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
			}
		}
		return sb.toString();
	}
}
