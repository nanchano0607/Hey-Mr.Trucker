import { useEffect, useMemo, useRef, useState } from "react";
import { useAuth } from "../../auth/useAuth";
import { API_BASE_URL } from "../../config/apiBase";
import api from "../../lib/axios";

type StockistItem = {
  id: number;
  imageUrl: string;
};

const SERVER = API_BASE_URL;

function chunkPairs(items: StockistItem[]) {
  const rows: StockistItem[][] = [];

  for (let i = 0; i < items.length; i += 2) {
    rows.push(items.slice(i, i + 2));
  }

  return rows;
}

export default function StockistPage() {
  const { user } = useAuth();
  const isAdmin = !!user?.isAdmin;

  const [items, setItems] = useState<StockistItem[]>([]);
  const [selectedImageUrl, setSelectedImageUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const displayBg = `${SERVER}/images/emptyload.webp`;

  const fetchStockists = async () => {
    setLoading(true);
    setError(null);

    try {
      const res = await api.get(`/api/stockist`);
      const data = res.data;

      if (Array.isArray(data)) {
        setItems(data as StockistItem[]);
      } else {
        setItems([]);
        setError("판매점 목록 응답 형식이 올바르지 않습니다.");
      }
    } catch (e: any) {
      console.error(e);
      setError(e?.message || "판매점 목록을 불러오는 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStockists();
  }, []);

  useEffect(() => {
    if (!selectedImageUrl) return;

    const previousOverflow = document.body.style.overflow;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setSelectedImageUrl(null);
      }
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", onKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [selectedImageUrl]);

  const rows = useMemo(() => chunkPairs(items), [items]);

  const handleAddClick = () => {
    if (!isAdmin) return;
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploading(true);
    setError(null);

    try {
      for (const file of Array.from(files)) {
        const formData = new FormData();
        formData.append("file", file);

        const uploadRes = await api.post(`/api/upload`, formData, {
          headers: { "Content-Type": "multipart/form-data" },
        });

        const imageUrl = uploadRes.data?.url as string | undefined;
        if (!imageUrl) {
          throw new Error("업로드 응답에 imageUrl이 없습니다.");
        }

        await api.post(`/api/stockist`, { imageUrl });
      }

      await fetchStockists();
    } catch (e: any) {
      console.error(e);
      setError(e?.response?.data || e?.message || "이미지 추가 중 오류가 발생했습니다.");
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleDelete = async (item: StockistItem) => {
    if (!window.confirm("이 판매점 이미지를 삭제하시겠습니까?")) return;

    setError(null);

    try {
      await api.delete(`/api/stockist/${item.id}`);

      try {
        await api.post(`/api/image/delete`, [item.imageUrl]);
      } catch (deleteFileError) {
        console.warn("판매점 이미지 파일 삭제 실패:", deleteFileError);
      }

      setItems((prev) => prev.filter((current) => current.id !== item.id));
    } catch (e: any) {
      console.error(e);
      setError(e?.response?.data || e?.message || "삭제 중 오류가 발생했습니다.");
    }
  };

  return (
    <section className="bg-[#FFFFF0] w-full relative min-h-screen">
      <div
        className="fixed inset-0 w-full min-h-screen bg-cover bg-center"
        style={{
          backgroundImage: `url('${displayBg}')`,
          zIndex: 0,
        }}
      />

      <div className="relative z-10 min-h-screen px-6 pt-32 pb-24 md:px-10">
        <div className="mx-auto max-w-6xl">
          {error && (
            <div className="mb-6 bg-red-50 px-4 py-3 text-sm text-red-700 shadow">
              {error}
            </div>
          )}

          {loading ? (
            <div className="py-20 text-center text-white text-xl font-beaver">
              LOADING...
            </div>
          ) : items.length === 0 ? (
            <div className="py-24 text-center text-white">
              <p className="font-beaver text-3xl tracking-wider">NO STOCKISTS YET</p>
              {isAdmin && (
                <p className="mt-3 text-sm text-white/80">
                  오른쪽 아래 버튼으로 판매점 이미지를 추가할 수 있습니다.
                </p>
              )}
            </div>
          ) : (
            <div className="space-y-6">
              {rows.map((row, rowIndex) => {
                const isLastSingle = row.length === 1 && rowIndex === rows.length - 1;

                return (
                  <div
                    key={`row-${rowIndex}`}
                    className={`flex gap-6 ${
                      isLastSingle
                        ? "justify-center"
                        : "flex-col items-center md:flex-row md:justify-center"
                    }`}
                  >
                    {row.map((item) => (
                      <div
                        key={item.id}
                        className="bg-transparent overflow-hidden  transition-all relative group w-full max-w-xl md:w-[calc(50%-12px)]"
                      >
                        {isAdmin && (
                          <button
                            onClick={(event) => {
                              event.stopPropagation();
                              handleDelete(item);
                            }}
                            className="absolute top-2 right-2 bg-red-600 text-white w-8 h-8 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-700 z-10"
                            title="판매점 삭제"
                          >
                            ✕
                          </button>
                        )}

                        <button
                          type="button"
                          onClick={() => setSelectedImageUrl(item.imageUrl)}
                          className="block w-full cursor-pointer active:scale-[0.99] transition-transform"
                        >
                          <img
                            src={item.imageUrl}
                            alt={`stockist-${item.id}`}
                            className="w-full h-86 object-contain bg-transparent"
                          />
                        </button>
                      </div>
                    ))}
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {isAdmin && (
          <>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              onChange={handleFileChange}
              className="hidden"
            />
            <button
              onClick={handleAddClick}
              disabled={uploading}
              className="fixed bottom-24 right-6 md:bottom-28 md:right-8 px-6 py-3 bg-blue-600 text-white shadow-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed font-beaver z-20"
              style={{ fontSize: "20px" }}
            >
              {uploading ? "업로드 중..." : "판매점 추가"}
            </button>
          </>
        )}
      </div>

      {selectedImageUrl && (
        <div
          className="fixed inset-0 z-[90] bg-black/70 px-4 py-8"
          onClick={(event) => {
            if (event.target === event.currentTarget) {
              setSelectedImageUrl(null);
            }
          }}
        >
          <div className="mx-auto flex min-h-full max-w-6xl items-center justify-center">
            <div className="relative w-full overflow-hidden bg-transparent">
              <button
                type="button"
                onClick={() => setSelectedImageUrl(null)}
                className="absolute right-2 top-2 z-10 flex h-10 w-10 items-center justify-center bg-black/55 text-2xl text-white transition hover:bg-black/75"
                aria-label="닫기"
              >
                ×
              </button>
              <img
                src={selectedImageUrl}
                alt="선택한 판매점 이미지"
                className="mx-auto max-h-[78vh] max-w-[88vw] object-contain"
              />
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
