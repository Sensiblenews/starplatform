package com.sensible.admin.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sensible.api.service.SuperAppService;

@Component
public class PageGeneratorScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PageGeneratorScheduler.class);

    @Autowired
    private SuperAppService superAppService;

    // --- Hard-coded Nickname Data ---
    // 🌟 글로벌하게 쓰기 좋은 닉네임용 수식어 10개 (공통 사용)
    private static final String[] PREFIXES = {
        "Cool", "Neon", "Sunny", "Lucky", "Brave", 
        "Charming", "Mighty", "Swift", "Cozy", "Dazzling"
    };

    private static final Map<String, String[]> FIRST_NAMES = new HashMap<>();

    static {
        // KR (South Korea - First Names) ~100+
        FIRST_NAMES.put("KR", new String[]{
            "Minsu", "Seoyeon", "Jihoon", "Haeun", "Doyun", "Jiwoo", "Hyunwoo", "Seoyun", "Gunwoo", "Jiyoon", 
            "Woojin", "Chaewon", "Minjun", "Yeeun", "Sunwoo", "Jimin", "Yuna", "Sujin", "Donghyun", "Seungho", 
            "Eunji", "Minji", "Hyunjin", "Subin", "Haeyoon", "Jonghyun", "Yujin", "Sora", "Kyungsoo", "Jaehyun", 
            "Taeyong", "Doyoung", "Eunwoo", "Seungyoon", "Mino", "Seunghoon", "Jisoo", "Jennie", "Nayeon", "Jeongyeon", 
            "Jihyo", "Mina", "Dahyun", "Chaeyoung", "Irene", "Seulgi", "Wendy", "Joy", "Yeri", "Karina", 
            "Winter", "Yeji", "Lia", "Ryujin", "Chaeryeong", "Wonyoung", "Gaeul", "Rei", "Liz", "Leeseo", 
            "Hanni", "Danielle", "Haerin", "Hyein", "Eunchae", "Sakura", "Yunjin", "Kazuha", "Soobin", "Yeonjun", 
            "Beomgyu", "Taehyun", "Heeseung", "Jay", "Jake", "Sunghoon", "Sunoo", "Jungwon", "Jinwoo", "Jungkook",
            "Taehyung", "Namjoon", "Hoseok", "Seokjin", "Yoongi", "Jihun", "Seonghwa", "Hongjoong", "Yunho", "Yeosang",
            "San", "Mingi", "Wooyoung", "Jongho", "Hyunsuk", "Jihoon", "Yoshi", "Junkyu", "Jaehyuk", "Asahi"
        });

        // JP (Japan - First Names) ~100+
        FIRST_NAMES.put("JP", new String[]{
            "Ren", "Himari", "Haruto", "Akari", "Minato", "Ichika", "Itsuki", "Sara", "Kaito", "Mio", 
            "Sakura", "Yuma", "Yui", "Sota", "Mei", "Kenji", "Yuki", "Aoi", "Hina", "Yuta", 
            "Daiki", "Takuya", "Riku", "Sora", "Hiroto", "Yuto", "Koki", "Ryo", "Sho", "Tsubasa", 
            "Yoshi", "Kenta", "Keita", "Ryota", "Tatsuya", "Kazuya", "Shota", "Jun", "Naoto", "Shin", 
            "Makoto", "Takeru", "Kosei", "Ryusei", "Asahi", "Hinata", "Touma", "Yusei", "Arata", "Ayato", 
            "Rui", "Saku", "Haruki", "Rito", "Kanata", "Reo", "Yuuma", "Sousuke", "Yuuhi", "Aki", 
            "Rin", "Kanna", "Yua", "Airi", "Riko", "Honoka", "Konomi", "Koharu", "Saki", "Nanami", 
            "Misaki", "Asuka", "Ayaka", "Haruka", "Natsuki", "Maki", "Rika", "Kana", "Miki", "Yoko", 
            "Tomoko", "Keiko", "Yuko", "Mariko", "Noriko", "Akiko", "Naoko", "Mayumi", "Hiromi", "Megumi", 
            "Ayumi", "Sayuri", "Kaori", "Saori", "Hikari", "Kokoro", "Shiori", "Suzu", "Wakana", "Yuriko"
        });

        // GB (United Kingdom - First Names) ~100+
        FIRST_NAMES.put("GB", new String[]{
            "Oliver", "Olivia", "George", "Amelia", "Noah", "Isla", "Harry", "Ava", "Leo", "Mia", 
            "Jack", "Sophia", "Freddie", "Grace", "Charlie", "Arthur", "Thomas", "Emily", "Sophie", "Oscar", 
            "Alfie", "Henry", "Archie", "Joshua", "William", "Jacob", "Ethan", "James", "Alexander", "Max", 
            "Isaac", "Lucas", "Teddy", "Finley", "Daniel", "Riley", "Harrison", "Tyler", "Mason", "Logan", 
            "Elijah", "Dylan", "Hunter", "Caleb", "Jaxon", "Ryan", "Nathan", "Christian", "Aaron", "Cameron", 
            "Toby", "Elliott", "Harvey", "Louis", "Callum", "Leon", "Matthew", "Luke", "David", "John", 
            "Michael", "Lily", "Evie", "Florence", "Poppy", "Ella", "Rosie", "Ivy", "Willow", "Daisy", 
            "Freya", "Chloe", "Sienna", "Matilda", "Evelyn", "Ruby", "Phoebe", "Harper", "Alice", "Jessica", 
            "Aria", "Mila", "Luna", "Layla", "Penelope", "Eleanor", "Scarlett", "Zara", "Molly", "Eva", 
            "Maisie", "Imogen", "Eliza", "Lucy", "Erin", "Lola", "Ellie", "Megan", "Hannah", "Abigail"
        });

        // US (United States - First Names) ~100+
        FIRST_NAMES.put("US", new String[]{
            "Liam", "Emma", "Noah", "Olivia", "James", "Charlotte", "Elijah", "Amelia", "William", "Sophia", 
            "Lucas", "Isabella", "Benjamin", "Mia", "Henry", "Evelyn", "Alexander", "Harper", "Michael", "Camila", 
            "Ethan", "Gianna", "Daniel", "Abigail", "Matthew", "Luna", "Aiden", "Ella", "Jackson", "Elizabeth", 
            "Logan", "Sofia", "David", "Avery", "Joseph", "Emily", "Samuel", "Chloe", "Sebastian", "Aria", 
            "Carter", "Scarlett", "Wyatt", "Victoria", "Jayden", "Madison", "John", "Grace", "Owen", "Penelope", 
            "Dylan", "Riley", "Luke", "Layla", "Gabriel", "Zoey", "Anthony", "Nora", "Isaac", "Lily", 
            "Grayson", "Eleanor", "Jack", "Hannah", "Julian", "Lillian", "Levi", "Addison", "Christopher", "Aubrey", 
            "Joshua", "Ellie", "Andrew", "Stella", "Lincoln", "Natalie", "Mateo", "Zoe", "Ryan", "Leah", 
            "Jaxon", "Hazel", "Nathan", "Violet", "Aaron", "Aurora", "Isaiah", "Savannah", "Thomas", "Audrey", 
            "Charles", "Brooklyn", "Caleb", "Bella", "Josiah", "Claire", "Christian", "Skylar", "Hunter", "Lucy"
        });

        // FR (France - First Names) ~100+
        FIRST_NAMES.put("FR", new String[]{
            "Gabriel", "Jade", "Leo", "Louise", "Raphael", "Alice", "Arthur", "Chloe", "Louis", "Emma", 
            "Adam", "Rose", "Jules", "Anna", "Hugo", "Ambre", "Lucas", "Lina", "Gabin", "Mia", 
            "Mael", "Julia", "Paul", "Elena", "Nathan", "Ines", "Mathis", "Mila", "Sacha", "Lea", 
            "Aaron", "Celeste", "Victor", "Agathe", "Leon", "Lena", "Gaston", "Juliette", "Martin", "Jeanne", 
            "Eden", "Lou", "Nael", "Zoe", "Marius", "Charlie", "Timothee", "Eva", "Isaac", "Nina", 
            "Ayden", "Romane", "Tom", "Victoria", "Eliott", "Lola", "Gauthier", "Margaux", "Simon", "Camille", 
            "Axel", "Olivia", "Nino", "Alix", "Gaspard", "Margot", "Tiago", "Lucie", "Romeo", "Lise", 
            "Malo", "Romy", "Ambroise", "Clara", "Antoine", "Sarah", "Clement", "Alicia", "Baptiste", "Apolline", 
            "Alexandre", "Iris", "Maxence", "Emy", "Yanis", "Capucine", "Evan", "Mathilde", "Elsa", "Youssef", 
            "Anais", "Enzo", "Lily", "Theo", "Lila", "Rayane", "Celia", "Ibrahim", "Maya", "Maelys", "Marguerite"
        });

        // DE (Germany - First Names) ~100+
        FIRST_NAMES.put("DE", new String[]{
            "Noah", "Mia", "Leon", "Emma", "Paul", "Sophia", "Jonas", "Hannah", "Elias", "Lea", 
            "Felix", "Marie", "Lukas", "Mila", "Henry", "Emilia", "Maximilian", "Lina", "Finn", "Anna", 
            "Ben", "Johanna", "Luis", "Clara", "Julian", "Luisa", "Moritz", "Leonie", "David", "Sophie", 
            "Tim", "Amelie", "Jakob", "Lena", "Luca", "Nele", "Anton", "Lara", "Oskar", "Mathilda", 
            "Philipp", "Lilly", "Alexander", "Maja", "Mats", "Charlotte", "Maxim", "Frieda", "Tom", "Melina", 
            "Emil", "Paula", "Milan", "Sarah", "Theo", "Pia", "Vincent", "Ida", "Leo", "Jana", 
            "Till", "Julia", "Jan", "Ella", "Jonathan", "Elisa", "Marlon", "Leni", "Linus", "Fiona", 
            "Jannis", "Alina", "Samuel", "Marlene", "Jona", "Zoe", "Mika", "Victoria", "Nico", "Carlotta", 
            "Leonard", "Theresa", "Fabian", "Antonia", "Simon", "Miriam", "Rafael", "Mara", "Erik", "Helena", 
            "Bastian", "Mona", "Oliver", "Ronja", "Florian", "Merle", "Kilian", "Sina", "Hannes", "Joline"
        });
    }

    /**
     * 🌟 매 10분 간격으로 (0분, 10분, 20분...) 국가별로 빈 페이지가 '최대 50개'가 되도록 부족한 만큼만 생성
     */
    @Scheduled(cron = "0 0/10 * * * *")
    public void generatePeriodicEmptyPages() {
        logger.info("🌟 [Scheduler] 주기적 빈 페이지(Empty Page) 재고 보충 시작");

        String[] countries = {"KR", "JP", "GB", "US", "FR", "DE"};
        int totalGenerated = 0;

        for (String country : countries) {
            try {
                // 1. 현재 해당 국가의 빈 페이지 개수 확인
                int currentEmptyCount = superAppService.countEmptyPages(country);
                
                // 2. 50개 중에서 부족한 개수 계산
                int neededCount = 50 - currentEmptyCount;

                // 3. 이미 50개 이상이라면 생성 패스
                if (neededCount <= 0) {
                    logger.info("✔️ [{}] 빈 페이지가 충분합니다 (현재 {}개). 생성을 건너뜁니다.", country, currentEmptyCount);
                    continue;
                }

                // 4. 부족한 개수만큼만 닉네임 추출 및 생성
                List<String> generatedNicknames = generateUniqueNicknames(country, neededCount);
                
                for (String nickname : generatedNicknames) {
                    Map<String, Object> param = new HashMap<>();
                    param.put("name", nickname);
                    param.put("country", country);
                    param.put("pageId", "SP-" + UUID.randomUUID().toString().substring(0, 13));

                    superAppService.createAutoPage(param);
                    totalGenerated++;
                }
                
                logger.info("✔️ [{}] 빈 페이지 {}개 보충 완료. (총 50개 유지)", country, neededCount);
                
            } catch (Exception e) {
                logger.error("[Scheduler] 페이지 생성 실패 (" + country + ") - " + e.getMessage());
            }
        }

        logger.info("🌟 [Scheduler] 주기적 빈 페이지 재고 보충 종료. (이번 턴에 새로 생성된 페이지: 총 {}개)", totalGenerated);
    }

    /**
     * 헬퍼 메서드: 수식어와 이름을 조합하여 중복 없는 닉네임 리스트 생성
     */
    private List<String> generateUniqueNicknames(String country, int targetCount) {
    	String[] firstNames = FIRST_NAMES.get(country);
        List<String> selectedNames = new ArrayList<>();
        Random random = new Random();

        // 요구한 개수만큼 무작위로 뽑아서 리스트에 담음
        for (int i = 0; i < targetCount; i++) {
            String randomName = firstNames[random.nextInt(firstNames.length)];
            
            // 🌟 필요하다면 PREFIXES 배열을 조합하여 "Sunny Minsu" 형태로도 확장 가능합니다.
            // String prefix = PREFIXES[random.nextInt(PREFIXES.length)];
            // selectedNames.add(prefix + " " + randomName);
            
            selectedNames.add(randomName);
        }

        return selectedNames;
    }
}