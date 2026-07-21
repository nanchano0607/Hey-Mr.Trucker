import { useEffect, useMemo, useState } from "react";
import { withBase } from "../config/apiBase";
import type { Popup } from "../types/popup";
import { resolvePopupImageUrl } from "../types/popup";

const STORAGE_KEY = "capshop-popup-hidden";

function getTodayString() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function wasHiddenToday(imageUrl: string) {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return false;
    }

    const parsed = JSON.parse(raw) as { date?: string; imageUrl?: string };
    return parsed.date === getTodayString() && parsed.imageUrl === imageUrl;
  } catch {
    return false;
  }
}

function hideForToday(imageUrl: string) {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      date: getTodayString(),
      imageUrl,
    })
  );
}

interface SitePopupProps {
  disabled?: boolean;
}

export default function SitePopup({ disabled = false }: SitePopupProps) {
  const [popup, setPopup] = useState<Popup | null>(null);
  const [open, setOpen] = useState(false);
  const [imageFailed, setImageFailed] = useState(false);

  const imageUrl = useMemo(
    () => resolvePopupImageUrl(popup?.imageUrl),
    [popup?.imageUrl]
  );

  useEffect(() => {
    if (disabled) {
      setOpen(false);
      return;
    }

    let cancelled = false;

    const fetchPopup = async () => {
      try {
        const response = await fetch(withBase("/api/popup/current"), {
          credentials: "include",
        });

        if (response.status === 204) {
          if (!cancelled) {
            setPopup(null);
            setOpen(false);
          }
          return;
        }

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const data = (await response.json()) as Popup;
        const nextImageUrl = resolvePopupImageUrl(data?.imageUrl);
        const shouldOpen =
          Boolean(data?.isActive ?? true) &&
          Boolean(nextImageUrl) &&
          !wasHiddenToday(nextImageUrl);

        if (!cancelled) {
          setPopup(data);
          setImageFailed(false);
          setOpen(shouldOpen);
        }
      } catch (error) {
        console.error("팝업 조회 실패", error);
        if (!cancelled) {
          setPopup(null);
          setOpen(false);
        }
      }
    };

    fetchPopup();

    return () => {
      cancelled = true;
    };
  }, [disabled]);

  useEffect(() => {
    if (!open) {
      return;
    }

    const previousOverflow = document.body.style.overflow;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", onKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  if (disabled || !open || !popup || !imageUrl || imageFailed) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-[90] bg-black/60 px-4 py-8"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          setOpen(false);
        }
      }}
    >
      <div className="mx-auto flex min-h-full max-w-xl items-center justify-center">
        <div className="w-full overflow-hidden bg-transparent shadow-[0_24px_80px_rgba(0,0,0,0.45)]">
          <div className="p-0">
            <img
              src={imageUrl}
              alt="진행 중인 팝업"
              className="max-h-[70vh] w-full object-contain"
              onError={() => setImageFailed(true)}
            />
          </div>

          <div className="mt-3 flex flex-col gap-2 sm:flex-row">
            <button
              type="button"
              onClick={() => {
                hideForToday(imageUrl);
                setOpen(false);
              }}
              className="flex-1 bg-[#fff7dc] px-4 py-3 text-sm font-medium text-gray-800 transition hover:bg-[#f5ebc8]"
            >
              오늘 하루 보지 않기
            </button>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="flex-1 bg-white px-4 py-3 text-sm font-medium text-gray-900 transition hover:bg-gray-100"
            >
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
