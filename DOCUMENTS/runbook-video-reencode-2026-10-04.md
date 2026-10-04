# 운영 영상 재인코딩 런북 — HEVC → H.264 (2026-10-04)

## 왜

운영 `/video/` 에 올라간 영상 중 아이폰 HEVC 원본은 PC Chrome·Firefox·다수 Android 에서 재생되지 않는다
(검토서 `review-2026-10-04-web-video-followup.md` 0절). 앞으로의 업로드는 서버가 자동 변환하지만(`VideoTranscodeUtil`),
이미 올라간 파일은 서버에서 한 번 손으로 바꿔야 한다.

2026-10-04 기준 확인된 HEVC 파일 2개:

| 글 | 파일 |
|---|---|
| /post/43 (MSK) | `18143c69c7cf4b95bf5c4ce581158731.mp4` |
| /post/41 (Emma) | `3fe1c99c07bd40abab3381b2e749b0eb.mp4` |

파일명을 그대로 유지하므로 DB·URL 은 바꾸지 않는다.

## 전제

- 서버 SSH 접속 (root 또는 tomcat7 소유자 권한)
- `ffmpeg`·`ffprobe` 가 서버에 있고 `libx264` 를 포함한다. 확인: `ffmpeg -hide_banner -encoders | grep libx264`
- 영상 디렉터리: `/var/lib/tomcat7/webapps/video/` (Constants._VIDEO_SAVE_PATH)

## 1. HEVC 전수 점검

```bash
cd /var/lib/tomcat7/webapps/video
for f in *.mp4 *.mov *.MOV; do
  [ -f "$f" ] || continue
  c=$(ffprobe -v error -select_streams v:0 -show_entries stream=codec_name -of csv=p=0 "$f")
  [ "$c" != "h264" ] && echo "$c $f"
done
```

`h264` 가 아닌 줄만 출력된다. (2026-10-04 기준 `hevc` 2건 예상)

## 2. 재인코딩 (파일당)

```bash
cd /var/lib/tomcat7/webapps/video
f=18143c69c7cf4b95bf5c4ce581158731.mp4
cp "$f" "/root/video-backup/$f"   # 백업 디렉터리는 미리 만들 것
ffmpeg -i "$f" -c:v libx264 -preset fast -crf 23 -pix_fmt yuv420p \
  -vf "scale=trunc(iw/2)*2:trunc(ih/2)*2" -c:a aac -b:a 128k -movflags +faststart \
  -y "$f.tmp.mp4" && mv "$f.tmp.mp4" "$f" && chown tomcat7:tomcat7 "$f"
```

옵션은 서버 코드(`VideoTranscodeUtil.buildCommand`)와 동일하다. 다른 파일은 `f=` 만 바꿔 반복한다.

## 3. 확인

```bash
ffprobe -v error -select_streams v:0 -show_entries stream=codec_name -of csv=p=0 "$f"   # h264
```

바깥에서: `curl -sI https://witch-hunting.com/video/$f | grep -i content-length` 로 크기가 바뀌었는지 본다.

## 4. Cloudflare 캐시 퍼지

영상은 CF 에 캐시된다(`cf-cache-status: HIT`, max-age 31일). 바꾼 파일 URL 을 CF 대시보드 → Caching → Purge by URL 로 지운다.

- `https://witch-hunting.com/video/18143c69c7cf4b95bf5c4ce581158731.mp4`
- `https://witch-hunting.com/video/3fe1c99c07bd40abab3381b2e749b0eb.mp4`

## 5. 끝 확인

PC Chrome 에서 `https://witch-hunting.com/?page=2` 의 Emma 카드(`/post/41`)가 스크롤 진입 시 재생되는지, 클릭 시 뷰어가 뜨는지 본다.
