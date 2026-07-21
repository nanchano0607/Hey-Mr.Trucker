import { useEffect, useMemo, useState } from "react";
import api from "../../lib/axios";
import type { Popup } from "../../types/popup";
import { resolvePopupImageUrl } from "../../types/popup";
import { withBase } from "../../config/apiBase";

interface PopupManagementProps {
  isOpen: boolean;
  onToggle: () => void;
}

function normalizeUploadUrl(data: { url?: string; filename?: string }) {
  if (data.url?.trim()) {
    return resolvePopupImageUrl(data.url);
  }

  if (data.filename?.trim()) {
    return withBase(`/uploads/cap/${encodeURIComponent(data.filename.trim())}`);
  }

  return "";
}

export default function PopupManagement({
  isOpen,
  onToggle,
}: PopupManagementProps) {
  const [currentPopup, setCurrentPopup] = useState<Popup | null>(null);
  const [imageUrl, setImageUrl] = useState("");
  const [isActive, setIsActive] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loadingCurrent, setLoadingCurrent] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const previewUrl = useMemo(
    () => resolvePopupImageUrl(imageUrl),
    [imageUrl]
  );

  const fetchCurrentPopup = async () => {
    setLoadingCurrent(true);
    setError("");

    try {
      const response = await api.get("/api/admin/popup", {
        validateStatus: (status) => status >= 200 && status < 300,
      });

      const data = (response.data ?? null) as Popup | null;
      setCurrentPopup(data);
      setImageUrl(data?.imageUrl?.trim() ?? "");
      setIsActive(Boolean(data?.isActive));
    } catch (fetchError: any) {
      console.error("관리자 팝업 조회 실패", fetchError);
      setError(
        fetchError?.response?.data?.message ||
          fetchError?.message ||
          "현재 팝업 정보를 불러오지 못했습니다."
      );
    } finally {
      setLoadingCurrent(false);
    }
  };

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    fetchCurrentPopup();
  }, [isOpen]);

  const handleUpload = async (file: File | null) => {
    if (!file) {
      return;
    }

    setUploading(true);
    setMessage("");
    setError("");

    try {
      const formData = new FormData();
      formData.append("file", file);

      const response = await api.post("/api/upload", formData, {
        validateStatus: (status) => status >= 200 && status < 300,
      });

      const uploadedUrl = normalizeUploadUrl(response.data ?? {});
      if (!uploadedUrl) {
        throw new Error("업로드 결과에서 이미지 URL을 찾지 못했습니다.");
      }

      setImageUrl(uploadedUrl);
      setMessage("이미지를 업로드했습니다. 저장 버튼을 누르면 팝업에 반영됩니다.");
    } catch (uploadError: any) {
      console.error("팝업 이미지 업로드 실패", uploadError);
      setError(
        uploadError?.response?.data?.message ||
          uploadError?.message ||
          "이미지 업로드에 실패했습니다."
      );
    } finally {
      setUploading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    setMessage("");
    setError("");

    try {
      const response = await api.post(
        "/api/admin/popup",
        {
          imageUrl: imageUrl.trim(),
          isActive,
        },
        {
          validateStatus: (status) => status >= 200 && status < 300,
        }
      );

      const data = (response.data ?? null) as Popup | null;
      setCurrentPopup(data);
      setImageUrl(data?.imageUrl?.trim() ?? imageUrl.trim());
      setIsActive(Boolean(data?.isActive));
      setMessage("팝업 정보를 저장했습니다.");
    } catch (saveError: any) {
      console.error("팝업 저장 실패", saveError);
      setError(
        saveError?.response?.data?.message ||
          saveError?.message ||
          "팝업 저장에 실패했습니다."
      );
    } finally {
      setSaving(false);
    }
  };

  const handleDeactivate = async () => {
    setSaving(true);
    setMessage("");
    setError("");

    try {
      const response = await api.post("/api/admin/popup/deactivate", null, {
        validateStatus: (status) => status >= 200 && status < 300,
      });

      const data = (response.data ?? null) as Popup | null;
      setCurrentPopup(data);
      setImageUrl(data?.imageUrl?.trim() ?? "");
      setIsActive(false);
      setMessage("팝업을 비활성화했습니다.");
    } catch (saveError: any) {
      console.error("팝업 비활성화 실패", saveError);
      setError(
        saveError?.response?.data?.message ||
          saveError?.message ||
          "팝업 비활성화에 실패했습니다."
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="border p-4 rounded font-sans">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold">팝업 관리</h2>
          <p className="mt-1 text-sm text-gray-600">
            현재 노출할 팝업 이미지를 저장하고 활성화 상태를 제어합니다.
          </p>
        </div>
        <button
          onClick={onToggle}
          className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
        >
          {isOpen ? "접기" : "펼치기"}
        </button>
      </div>

      {isOpen && (
        <div className="mt-4 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2 rounded border bg-white/60 p-3 text-sm">
            <div className="flex items-center gap-2">
              <span className="font-medium text-gray-700">현재 상태</span>
              <span
                className={`rounded-full px-2 py-1 text-xs font-semibold ${
                  currentPopup?.isActive
                    ? "bg-green-100 text-green-700"
                    : "bg-gray-200 text-gray-700"
                }`}
              >
                {currentPopup?.isActive ? "활성" : "비활성"}
              </span>
            </div>
            <button
              type="button"
              onClick={fetchCurrentPopup}
              disabled={loadingCurrent || uploading || saving}
              className="px-3 py-1 border rounded text-sm bg-gray-100 hover:bg-gray-200 disabled:opacity-60"
            >
              {loadingCurrent ? "불러오는 중..." : "현재 설정 새로고침"}
            </button>
          </div>

          <div className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
            <div className="space-y-3 rounded border bg-white p-4">
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-gray-700">
                  팝업 이미지 URL
                </span>
                <input
                  type="text"
                  value={imageUrl}
                  onChange={(event) => setImageUrl(event.target.value)}
                  placeholder="https://... 또는 /uploads/..."
                  className="w-full rounded border px-3 py-2 text-sm"
                />
              </label>

              <label className="block">
                <span className="mb-2 block text-sm font-medium text-gray-700">
                  이미지 업로드
                </span>
                <input
                  type="file"
                  accept="image/*"
                  onChange={(event) => {
                    const file = event.target.files?.[0] ?? null;
                    void handleUpload(file);
                    event.currentTarget.value = "";
                  }}
                  className="w-full text-sm"
                  disabled={uploading || saving}
                />
              </label>

              <label className="flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={isActive}
                  onChange={(event) => setIsActive(event.target.checked)}
                />
                팝업 활성화
              </label>

              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={handleSave}
                  disabled={saving || uploading || !imageUrl.trim()}
                  className="rounded bg-black px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
                >
                  {saving ? "저장 중..." : "팝업 저장"}
                </button>
                <button
                  type="button"
                  onClick={handleDeactivate}
                  disabled={saving || uploading}
                  className="rounded border border-black/15 px-4 py-2 text-sm font-medium text-gray-700 disabled:opacity-60"
                >
                  팝업 비활성화
                </button>
              </div>

              {message && (
                <div className="rounded border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">
                  {message}
                </div>
              )}
              {error && (
                <div className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                  {error}
                </div>
              )}
            </div>

            <div className="rounded border bg-white p-4">
              <h3 className="text-sm font-semibold text-gray-700">미리보기</h3>
              <div className="mt-3 flex min-h-[240px] items-center justify-center overflow-hidden rounded-xl bg-[#f5edd7] p-3">
                {previewUrl ? (
                  <img
                    src={previewUrl}
                    alt="팝업 미리보기"
                    className="max-h-[320px] w-full rounded-lg object-contain bg-white"
                  />
                ) : (
                  <p className="text-sm text-gray-500">
                    이미지 URL을 입력하거나 파일을 업로드해주세요.
                  </p>
                )}
              </div>

              <div className="mt-3 text-xs text-gray-500">
                사용자 화면에서는 팝업이 전체 화면 오버레이로 노출되고,
                닫기 또는 오늘 하루 보지 않기를 선택할 수 있습니다.
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
