import { useEffect, useMemo, useRef, useState } from "react";
import { API_BASE_URL } from "../../config/apiBase";
import api from "../../lib/axios";

const SERVER = API_BASE_URL;

type StoryContentResponse = {
  url?: string;
};

type StoryBackgroundResponse = {
  url?: string;
};

function isVideoUrl(url: string) {
  const clean = (url || "").split(/[?#]/)[0].toLowerCase();
  return clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".ogg");
}

function filenameFromUrl(url: string) {
  const clean = (url || "").split(/[?#]/)[0];
  const idx = clean.lastIndexOf("/");
  return idx >= 0 ? clean.substring(idx + 1) : clean;
}

async function uploadToCap(file: File): Promise<string> {
  const fd = new FormData();
  fd.append("file", file);

  try {
    const res = await api.post(`/api/admin/upload`, fd, {
      // Content-Type은 자동으로 FormData boundary로 설정되도록 비워둡니다.
      validateStatus: (s) => s >= 200 && s < 300,
    });

    const data = res.data as { url?: string; filename?: string };
    if (data?.url) return data.url;

    // 일부 서버는 filename만 반환할 수 있음. 이 경우 public URL로 보정.
    if (data?.filename) {
      const name = (data.filename || "").trim();
      if (name) return `${SERVER}/uploads/cap/${encodeURIComponent(name)}`; // 서버 설정에 맞춰 필요시 조정
    }

    throw new Error("upload_no_url");
  } catch (e: any) {
    const status = e?.response?.status;
    const msg = e?.response?.data?.message || e?.message || "unknown";
    throw new Error(`upload_failed: ${status ?? ""} ${msg}`);
  }
}

async function deleteImages(namesOrUrls: string[]): Promise<{ success: string[]; fail: string[] }>
{
  try {
    const res = await api.post(
      `/api/admin/image/delete`,
      namesOrUrls,
      { validateStatus: (s) => s >= 200 && s < 300 }
    );
    const data = res.data as { success?: string[]; fail?: string[] };
    return {
      success: data.success ?? [],
      fail: data.fail ?? [],
    };
  } catch (e) {
    console.error("deleteImages error", e);
    return { success: [], fail: namesOrUrls };
  }
}

interface PendingFilePickerProps {
  accept: string;
  file: File | null;
  onChange: (file: File | null) => void;
  onApply: () => void;
  disabled: boolean;
  previewClassName: string;
}

/** '설정하기'로 파일을 고르면 적용 전 미리보기를 보여주고, '적용'으로 서버에 반영한다. */
function PendingFilePicker({
  accept,
  file,
  onChange,
  onApply,
  disabled,
  previewClassName,
}: PendingFilePickerProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [previewUrl, setPreviewUrl] = useState<string>("");

  useEffect(() => {
    if (!file) {
      setPreviewUrl("");
      // 같은 파일을 다시 고르더라도 change 이벤트가 발생하도록 입력값을 비운다.
      if (inputRef.current) inputRef.current.value = "";
      return;
    }
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  const previewIsVideo = !!file && file.type.startsWith("video/");

  return (
    <div className="mt-3 space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <input
          ref={inputRef}
          type="file"
          accept={accept}
          onChange={(e) => onChange(e.target.files?.[0] ?? null)}
          className="hidden"
        />
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={disabled}
          className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200 disabled:opacity-60"
        >
          설정하기
        </button>
        <span className="text-sm text-gray-700 break-all">
          {file ? file.name : "선택된 파일 없음"}
        </span>
      </div>

      {file && previewUrl && (
        <div>
          <div className="text-sm text-gray-700 mb-2">적용 전 미리보기:</div>
          <div className={`w-full bg-gray-200 rounded overflow-hidden ${previewClassName}`}>
            {previewIsVideo ? (
              <video
                src={previewUrl}
                className="w-full h-full object-cover"
                autoPlay
                loop
                muted
                playsInline
                controls
              />
            ) : (
              <img src={previewUrl} alt="적용 전 미리보기" className="w-full h-full object-cover" />
            )}
          </div>
        </div>
      )}

      <button
        type="button"
        onClick={onApply}
        disabled={disabled || !file}
        className="px-4 py-1.5 rounded text-sm font-semibold bg-blue-600 text-white hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        적용
      </button>
    </div>
  );
}

interface StoryManagementProps {
  isOpen: boolean;
  onToggle: () => void;
}

export default function StoryManagement({ isOpen, onToggle }: StoryManagementProps) {
  const [bgUrl, setBgUrl] = useState<string>("");
  const [contentUrl, setContentUrl] = useState<string>("");

  const [bgFile, setBgFile] = useState<File | null>(null);
  const [contentFile, setContentFile] = useState<File | null>(null);

  const [loading, setLoading] = useState<boolean>(false);

  const contentIsVideo = useMemo(() => isVideoUrl(contentUrl), [contentUrl]);

  const fetchCurrent = async () => {
    try {
          const res = await api.get(`/api/story`, { validateStatus: (s) => s >= 200 && s < 300 });
      const data = res.data as { backgroundImage?: string; contentImage?: string };

      const toUrl = (name?: string) =>
        name && name.trim()
          ? `${SERVER}/uploads/cap/${encodeURIComponent(name.trim())}`
          : "";

      setBgUrl(toUrl(data?.backgroundImage));
      setContentUrl(toUrl(data?.contentImage));
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    if (isOpen) fetchCurrent();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  const applyBackground = async () => {
    if (!bgFile) {
      alert("배경 파일을 선택해주세요.");
      return;
    }

    setLoading(true);
    try {
      // 1) 기존 배경 파일 삭제 시도 (URL 또는 이름 전달 가능)
      if (bgUrl) {
        const del = await deleteImages([bgUrl]);
        if (del.fail.length) {
          console.warn("배경 삭제 실패:", del.fail);
        }
      }

      // 2) 새 파일 업로드
      const uploadedUrl = await uploadToCap(bgFile);
      const filename = filenameFromUrl(uploadedUrl);

      // 3) 스토리 엔티티에 파일명 저장
      const res = await api.post(
        `/api/admin/story/background`,
        null,
        { params: { filename }, validateStatus: (s) => s >= 200 && s < 300 }
      );

      const data = res.data as StoryBackgroundResponse;
      setBgUrl(data?.url ?? uploadedUrl);
      alert("스토리 배경이 적용되었습니다.");
      setBgFile(null);
    } catch (e) {
      console.error(e);
      alert(
        `스토리 배경 업로드/적용 중 오류가 발생했습니다.\n${
          (e as any)?.message || ""
        }`
      );
    } finally {
      setLoading(false);
    }
  };

  const applyContent = async () => {
    if (!contentFile) {
      alert("콘텐츠 파일(이미지/동영상)을 선택해주세요.");
      return;
    }

    setLoading(true);
    try {
      // 1) 기존 콘텐츠 파일 삭제 시도
      if (contentUrl) {
        const del = await deleteImages([contentUrl]);
        if (del.fail.length) {
          console.warn("콘텐츠 삭제 실패:", del.fail);
        }
      }

      // 2) 새 파일 업로드
      const uploadedUrl = await uploadToCap(contentFile);
      const filename = filenameFromUrl(uploadedUrl);

      // 3) 스토리 엔티티에 파일명 저장
      const res = await api.post(
        `/api/admin/story/content`,
        null,
        { params: { filename }, validateStatus: (s) => s >= 200 && s < 300 }
      );

      const data = res.data as StoryContentResponse;
      setContentUrl(data?.url ?? uploadedUrl);
      alert("스토리 콘텐츠가 적용되었습니다.");
      setContentFile(null);
    } catch (e) {
      console.error(e);
      alert(
        `스토리 콘텐츠 업로드/적용 중 오류가 발생했습니다.\n${
          (e as any)?.message || ""
        }`
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="border p-4 rounded font-sans">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-xl font-semibold">스토리 관리</h2>
        </div>
        <button
          onClick={onToggle}
          className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
        >
          {isOpen ? "접기" : "펼치기"}
        </button>
      </div>

      {isOpen && (
        <div className="mt-4 space-y-6">
          <div className="border rounded p-3">
            <div className="flex items-center justify-between gap-2">
              <h3 className="font-semibold">배경</h3>
              <button
                onClick={fetchCurrent}
                disabled={loading}
                className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200 disabled:opacity-60"
              >
                새로고침
              </button>
            </div>

            <div className="mt-3">
              <div className="text-sm text-gray-700 mb-2">현재 배경:</div>
              <div className="w-full h-40 bg-gray-200 rounded overflow-hidden">
                {bgUrl ? (
                  <div
                    className="w-full h-full bg-cover bg-center"
                    style={{ backgroundImage: `url('${bgUrl}')` }}
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-sm text-gray-600">
                    설정된 배경이 없습니다.
                  </div>
                )}
              </div>
            </div>

            <PendingFilePicker
              accept="image/*"
              file={bgFile}
              onChange={setBgFile}
              onApply={applyBackground}
              disabled={loading}
              previewClassName="h-40"
            />
          </div>

          <div className="border rounded p-3">
            <div className="flex items-center justify-between gap-2">
              <h3 className="font-semibold">테두리 안 콘텐츠</h3>
            </div>

            <div className="mt-3">
              <div className="text-sm text-gray-700 mb-2">현재 콘텐츠:</div>
              <div className="w-full h-56 bg-gray-200 rounded overflow-hidden">
                {contentUrl ? (
                  contentIsVideo ? (
                    <video
                      src={contentUrl}
                      className="w-full h-full object-cover"
                      autoPlay
                      loop
                      muted
                      playsInline
                      controls
                    />
                  ) : (
                    <img
                      src={contentUrl}
                      alt="story content"
                      className="w-full h-full object-cover"
                    />
                  )
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-sm text-gray-600">
                    설정된 콘텐츠가 없습니다.
                  </div>
                )}
              </div>
            </div>

            <PendingFilePicker
              accept="image/*,video/*"
              file={contentFile}
              onChange={setContentFile}
              onApply={applyContent}
              disabled={loading}
              previewClassName="h-56"
            />
          </div>
        </div>
      )}
    </div>
  );
}
