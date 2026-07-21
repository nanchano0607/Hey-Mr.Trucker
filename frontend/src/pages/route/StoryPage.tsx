import { API_BASE_URL } from "../../config/apiBase";
import api from "../../lib/axios";
import { useEffect, useMemo, useState } from "react";

const SERVER = (API_BASE_URL || "").replace(/\/$/, "");

function isVideoUrl(url: string) {
  const clean = (url || "").split(/[?#]/)[0].toLowerCase();
  return clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".ogg");
}

export default function StoryPage() {
  // ✅ 기본 배경 "이미지"는 제거, 폴백은 "검은색"으로만 처리
  const [bgUrl, setBgUrl] = useState<string>("");
  const [contentUrl, setContentUrl] = useState<string>("");

  const contentIsVideo = useMemo(() => isVideoUrl(contentUrl), [contentUrl]);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get(`/api/story`, {
          validateStatus: (s) => s >= 200 && s < 300,
        });

        const data = res.data as {
          backgroundImage?: string;
          contentImage?: string;
        };

        const toUrl = (name?: string) =>
          name && name.trim()
            ? `${SERVER}/uploads/cap/${encodeURIComponent(name.trim())}`
            : "";

        const bg = toUrl(data.backgroundImage);
        const ct = toUrl(data.contentImage);

        if (bg) setBgUrl(bg);
        if (ct) setContentUrl(ct);
      } catch {
        // ignore (폴백은 검은색이므로 문제 없음)
      }
    };

    load();
  }, []);

  return (
    <>
      {/* ================= PC (md 이상): MyGarage와 동일 테두리 컨테이너 ================= */}
      <div className="hidden md:block fixed inset-0 overflow-hidden">
        {/* ✅ 배경: bgUrl 없으면 그냥 검은색 */}
        <div
          className="fixed inset-0 w-full h-full bg-black"
          style={{
            backgroundImage: bgUrl ? `url('${bgUrl}')` : "none",
            backgroundSize: "cover",
            backgroundPosition: "center",
            zIndex: 0,
          }}
        />

        <div
          className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 h-full flex items-center justify-center"
          style={{ zIndex: 1, paddingTop: "10vh" }}
        >
          <div
            className="relative bg-[#01132c] ml-6"
            style={{
              imageRendering: "pixelated",
              clipPath: `polygon(
                0% 20px, 20px 20px, 20px 0%,
                calc(100% - 20px) 0%, calc(100% - 20px) 20px, 100% 20px,
                100% calc(100% - 20px), calc(100% - 20px) calc(100% - 20px), calc(100% - 20px) 100%,
                20px 100%, 20px calc(100% - 20px), 0% calc(100% - 20px)
              )`,
              padding: "20px",
              width: "80vw",
            }}
          >
            <div
              className="w-full bg-black"
              style={{
                imageRendering: "pixelated",
                clipPath: `polygon(
                  0% 16px, 16px 16px, 16px 0%,
                  calc(100% - 16px) 0%, calc(100% - 16px) 16px, 100% 16px,
                  100% calc(100% - 16px), calc(100% - 16px) calc(100% - 16px), calc(100% - 16px) 100%,
                  16px 100%, 16px calc(100% - 16px), 0% calc(100% - 16px)
                )`,
                height: "60vh",
              }}
            >
              {contentUrl ? (
                contentIsVideo ? (
                  <video
                    src={contentUrl}
                    className="w-full h-full object-cover"
                    autoPlay
                    loop
                    muted
                    playsInline
                  />
                ) : (
                  <img
                    src={contentUrl}
                    alt="story content"
                    className="w-full h-full object-cover"
                  />
                )
              ) : (
                // ✅ contentUrl이 없으면 bgUrl이 있으면 bgUrl로, 없으면 검은색
                <div
                  className="w-full h-full bg-black bg-cover bg-center"
                  style={{
                    backgroundImage: bgUrl ? `url('${bgUrl}')` : "none",
                  }}
                />
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ================= 모바일 (md 미만): 단순 배경 + 컨텐츠 오버레이 ================= */}
      <section className="block md:hidden w-full min-h-screen">
        <div
          className="fixed inset-0 w-full h-full bg-black bg-cover bg-center"
          style={{
            backgroundImage: bgUrl ? `url('${bgUrl}')` : "none",
          }}
        />

        {contentUrl && (
          <div className="fixed inset-0 w-full h-full">
            {contentIsVideo ? (
              <video
                src={contentUrl}
                className="w-full h-full object-cover"
                autoPlay
                loop
                muted
                playsInline
              />
            ) : (
              <img
                src={contentUrl}
                alt="story content"
                className="w-full h-full object-cover"
              />
            )}
          </div>
        )}
      </section>
    </>
  );
}
