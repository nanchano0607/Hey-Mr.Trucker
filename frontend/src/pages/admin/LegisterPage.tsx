//상품등록
import { useState } from "react";
import { API_BASE_URL } from "../../config/apiBase";

type ProductType = "CAP" | "ACC" | "VINTAGE";
type VintageCategory = "CAP" | "SHIRT";

export default function RegisterPage() {
  const [productType, setProductType] = useState<ProductType>("CAP");
  const [vintageCategory, setVintageCategory] = useState<VintageCategory>("CAP");
  const [name, setName] = useState("");
  const [price, setPrice] = useState("");
  const [color, setColor] = useState("");
  const [mainImage, setMainImage] = useState<File | null>(null);
  const [images, setImages] = useState<File[]>([]);
  const [sizes, setSizes] = useState<string[]>([]);
  const [sizeInput, setSizeInput] = useState("");
  const [sizeInfo, setSizeInfo] = useState(
    '[{"size":"FREE","둘레":"54-56cm","챙 길이":"6.5cm","깊이":"11cm"}]'
  );
  const [sizeStocks, setSizeStocks] = useState<{ [size: string]: number }>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const cleanupUploadedImages = async (urls: string[]) => {
    const token = localStorage.getItem("access_token");
    if (!token) return;
    const namesOrUrls = (urls || []).map((u) => (u || "").trim()).filter(Boolean);
    if (namesOrUrls.length === 0) return;

    try {
      // 서버에 공용 삭제 엔드포인트가 존재하는 것으로 보임 (/api/image/delete)
      await fetch(`${API_BASE_URL}/api/image/delete`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(namesOrUrls),
      });
    } catch (e) {
      console.warn("uploaded image cleanup failed", e);
    }
  };

  const handleImagesChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setImages(Array.from(e.target.files));
    }
  };

  const addSize = () => {
    if (sizeInput.trim() && !sizes.includes(sizeInput.trim())) {
      const newSize = sizeInput.trim();
      setSizes([...sizes, newSize]);
      setSizeStocks({ ...sizeStocks, [newSize]: 0 });
      setSizeInput("");
    }
  };

  const removeSize = (sizeToRemove: string) => {
    setSizes(sizes.filter(size => size !== sizeToRemove));
    const newSizeStocks = { ...sizeStocks };
    delete newSizeStocks[sizeToRemove];
    setSizeStocks(newSizeStocks);
  };

  const updateSizeStock = (size: string, stock: number) => {
    setSizeStocks({ ...sizeStocks, [size]: stock });
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmitting) return;
    setIsSubmitting(true);

    // 사이즈 및 재고 검증
    if (sizes.length === 0) {
      alert("최소 하나의 사이즈를 추가해주세요.");
      setIsSubmitting(false);
      return;
    }

    // 모든 사이즈에 재고가 설정되었는지 확인
    const unsetSizes = sizes.filter(size => !sizeStocks[size] || sizeStocks[size] <= 0);
    if (unsetSizes.length > 0) {
      alert(`다음 사이즈의 재고를 설정해주세요: ${unsetSizes.join(", ")}`);
      setIsSubmitting(false);
      return;
    }

    try {
      const formData = new FormData();

      // 백엔드에 공용 업로드는 없고, CAP 업로드만 제공됨
      const uploadEndpoint = `${API_BASE_URL}/api/product/upload`;

      if (mainImage) {
        formData.append("mainImage", mainImage);
      }
      images.forEach((file: File) => {
        formData.append("images", file);
      });

      const token = localStorage.getItem("access_token");

      // 1) 이미지 업로드
      const uploadRes = await fetch(uploadEndpoint, {
        method: "POST",
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: formData,
      });
      if (!uploadRes.ok) {
        alert("이미지 업로드에 실패했습니다. 서버 엔드포인트/권한을 확인해주세요.");
        return;
      }

      const uploadedFileUrls = (await uploadRes.json()) as {
        mainImageUrl: string;
        imageUrls: string[];
      };

      const productData = {
        name,
        price,
        color,
        size: sizes,
        sizeInfo,
        sizeStocks,
        mainImageUrl: uploadedFileUrls.mainImageUrl,
        imageUrls: uploadedFileUrls.imageUrls,
        productType,
        ...(productType === "VINTAGE" ? { vintageCategory } : {}),
      };

      // 2) 상품 저장
      const saveEndpoint =
        productType === "CAP"
          ? `${API_BASE_URL}/api/cap/save`
          : productType === "ACC"
          ? `${API_BASE_URL}/api/acc/save`
          : `${API_BASE_URL}/api/vintage/save`;

      const saveRes = await fetch(saveEndpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(productData),
      });

      if (!saveRes.ok) {
        // 저장 실패 시: 방금 업로드한 이미지들 삭제(보상 트랜잭션)
        await cleanupUploadedImages([
          uploadedFileUrls.mainImageUrl,
          ...(uploadedFileUrls.imageUrls ?? []),
        ]);

        const errText = await saveRes.text().catch(() => "");
        alert(`상품 저장에 실패했습니다. (status: ${saveRes.status})\n${errText}`);
        return;
      }

      alert("등록 완료!");

      // 모든 입력 필드 초기화
      setProductType("CAP");
      setVintageCategory("CAP");
      setName("");
      setPrice("");
      setColor("");
      setMainImage(null);
      setImages([]);
      setSizes([]);
      setSizeInput("");
      setSizeInfo("");
      setSizeStocks({});
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-red-500 pt-8 pb-16 font-sans">
      <div className="max-w-lg mx-auto px-4">
        <h2 className="text-2xl font-bold mb-6">상품 등록</h2>
        <form onSubmit={handleRegister} className="space-y-4">
        
        {/* 상품 타입 선택 */}
        <div>
          <label className="block mb-1 font-medium">상품 타입 *</label>
          <select
            value={productType}
            onChange={(e) => setProductType(e.target.value as ProductType)}
            className="w-full border rounded px-3 py-2"
            required
          >
            <option value="CAP">모자 (CAP)</option>
            <option value="ACC">액세서리 (ACC)</option>
            <option value="VINTAGE">빈티지 (VINTAGE)</option>
          </select>
        </div>

        {productType === "VINTAGE" && (
          <div>
            <label className="block mb-1 font-medium">빈티지 분류 *</label>
            <select
              value={vintageCategory}
              onChange={(e) => setVintageCategory(e.target.value as VintageCategory)}
              className="w-full border rounded px-3 py-2"
              required
            >
              <option value="CAP">빈티지 모자</option>
              <option value="SHIRT">빈티지 티셔츠</option>
            </select>
          </div>
        )}

        <div>
          <label className="block mb-1 font-medium">상품명</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full border rounded px-3 py-2"
            required
          />
        </div>
        <div>
          <label className="block mb-1 font-medium">가격</label>
          <input
            type="number"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            className="w-full border rounded px-3 py-2 [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
            required
          />
        </div>
        <div>
          <label className="block mb-1 font-medium">색상</label>
          <input
            type="text"
            value={color}
            onChange={(e) => setColor(e.target.value)}
            className="w-full border rounded px-3 py-2"
          />
        </div>
        <div>
          <label className="block mb-1 font-medium">사이즈 및 재고</label>
          <div className="space-y-2">
            <div className="flex gap-2">
              <input
                type="text"
                value={sizeInput}
                onChange={(e) => setSizeInput(e.target.value)}
                placeholder="예: S, M, L 또는 FREE"
                className="flex-1 border rounded px-3 py-2"
                onKeyPress={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    addSize();
                  }
                }}
              />
              <button
                type="button"
                onClick={addSize}
                className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
              >
                사이즈 추가
              </button>
            </div>
            {sizes.length > 0 && (
              <div className="space-y-2">
                <div className="text-sm font-medium text-gray-700">사이즈별 재고 설정:</div>
                {sizes.map((size, index) => (
                  <div
                    key={index}
                    className="flex items-center gap-2 p-3 bg-gray-50 rounded border"
                  >
                    <span className="font-medium min-w-[60px]">{size}</span>
                    <input
                      type="number"
                      value={sizeStocks[size] || ""}
                      onChange={(e) => updateSizeStock(size, parseInt(e.target.value) || 0)}
                      placeholder="재고 수량"
                      className="flex-1 border rounded px-2 py-1 text-center"
                      min="0"
                      required
                    />
                    <span className="text-sm text-gray-600">개</span>
                    <button
                      type="button"
                      onClick={() => removeSize(size)}
                      className="text-red-500 hover:text-red-700 font-bold px-2"
                    >
                      삭제
                    </button>
                  </div>
                ))}
                <div className="text-xs text-gray-600">
                  * 각 사이즈별로 재고를 설정해주세요. 모든 사이즈의 재고는 필수입니다.
                </div>
              </div>
            )}
          </div>
        </div>
        <div>
          <label className="block mb-1 font-medium">사이즈 정보 (JSON 배열 형식)</label>
          <textarea
            value={sizeInfo}
            onChange={(e) => setSizeInfo(e.target.value)}
            placeholder='예시가 입력되어 있습니다. 숫자만 수정하세요.'
            className="w-full border rounded px-3 py-2 font-mono text-sm"
            rows={6}
          />
          <div className="text-xs text-gray-600 mt-1">
            * JSON 배열 형식으로 입력해주세요. 예시가 미리 채워져 있으니 필요한 숫자만 수정하시면 됩니다.
          </div>
        </div>
        <div>
          <label className="block mb-1 font-medium">메인 사진</label>
          <input
            type="file"
            accept="image/*"
            onChange={(e) => setMainImage(e.target.files?.[0] ?? null)}
            className="w-full"
            required
          />
        </div>
        <div>
          <label className="block mb-1 font-medium">상품 사진 (여러 장)</label>
          <input
            type="file"
            accept="image/*"
            multiple
            onChange={handleImagesChange}
            className="w-full"
          />
        </div>
        {/* 사이즈별 재고 관리로 대체됨 */}
        <button
          type="submit"
          className="w-full py-2 bg-slate-900 text-white rounded font-semibold"
        >
          등록하기
        </button>
      </form>
      </div>
    </div>
  );
}
