import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { API_BASE_URL } from "../../config/apiBase";

const SERVER = API_BASE_URL;

type ProductType = "CAP" | "ACC" | "VINTAGE";
type VintageCategory = "CAP" | "SHIRT";
type VintageCategoryFilter = "ALL" | VintageCategory;

type Product = {
  id: number;
  name: string;
  price: number;
  pastPrice?: number;
  mainImageUrl: string;
  color: string;
  stock?: number;
  stocks?: ProductStock[];
  productType?: ProductType;
  vintageCategory?: VintageCategory;
  status?: "ACTIVE" | "INACTIVE";
};

type ProductStock = {
  size: string;
  stock: number;
};

interface ProductManagementProps {
  isOpen: boolean;
  onToggle: () => void;
}

export default function ProductManagement({
  isOpen,
  onToggle,
}: ProductManagementProps) {
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedType, setSelectedType] = useState<ProductType>("CAP");
  const [selectedVintageCategory, setSelectedVintageCategory] =
    useState<VintageCategoryFilter>("ALL");
  const [statusTab, setStatusTab] = useState<"active" | "inactive">("active");
  const [productImages, setProductImages] = useState<Record<number, string[]>>(
    {}
  );
  const [isImageModalOpen, setIsImageModalOpen] = useState(false);
  const [modalType, setModalType] = useState<"main" | "details">("main");
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [newMainImage, setNewMainImage] = useState<File | null>(null);
  const [newImages, setNewImages] = useState<File[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    fetchProducts();
  }, []);

  useEffect(() => {
    if (isOpen) fetchProducts();
  }, [isOpen, selectedType]);

  useEffect(() => {
    if (!products.length) return;

    products.forEach((product) => {
      fetchProductImages(product.id);
    });
  }, [products]);

  useEffect(() => {
    if (selectedType !== "VINTAGE") {
      setSelectedVintageCategory("ALL");
    }
  }, [selectedType]);

  const normalizeImageUrl = (url: string) => {
    if (!url) return "";
    if (url.startsWith("http://") || url.startsWith("https://")) return url;
    return `${SERVER}${url}`;
  };

  const getVintageCategoryLabel = (category?: VintageCategory) => {
    if (category === "CAP") return "빈티지 모자";
    if (category === "SHIRT") return "빈티지 티셔츠";
    return "빈티지 미분류";
  };

  const statusFilteredProducts = products.filter((product) =>
    statusTab === "active"
      ? product.status !== "INACTIVE"
      : product.status === "INACTIVE"
  );

  const visibleProducts = statusFilteredProducts.filter((product) => {
    if (selectedType !== "VINTAGE" || selectedVintageCategory === "ALL") {
      return true;
    }
    return product.vintageCategory === selectedVintageCategory;
  });

  const fetchProducts = async () => {
    try {
      const token = localStorage.getItem("access_token");
      let endpoint = "";
      if (selectedType === "CAP") {
        endpoint = "/api/admin/products/type/CAP";
      } else if (selectedType === "ACC") {
        endpoint = "/api/admin/products/type/ACC";
      } else if (selectedType === "VINTAGE") {
        endpoint = "/api/admin/products/type/VINTAGE";
      }

      const res = await fetch(`${SERVER}${endpoint}`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });

      if (res.ok) {
        const data = await res.json();
        setProducts(data);
      }
    } catch (e) {
      console.error("상품 목록 조회 실패:", e);
    }
  };

  const fetchProductImages = async (productId: number) => {
    try {
      const token = localStorage.getItem("access_token");
      const res = await fetch(`${SERVER}/api/product/image_urls/${productId}`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });

      if (!res.ok) {
        console.error(`상품(${productId}) 이미지 조회 실패:`, res.status);
        return;
      }

      const data: string[] = await res.json();

      setProductImages((prev) => ({
        ...prev,
        [productId]: data ?? [],
      }));
    } catch (e) {
      console.error(`상품(${productId}) 이미지 조회 실패:`, e);
    }
  };

  const handleUpdateSizeStock = async (
    productId: number,
    size: string,
    currentStock: number
  ) => {
    const newStock = prompt(
      `"${size}" 사이즈 재고 수량을 입력하세요 (현재: ${currentStock}):`,
      String(currentStock)
    );
    if (newStock === null) return;

    const stockNumber = parseInt(newStock);
    if (isNaN(stockNumber) || stockNumber < 0) {
      alert("올바른 숫자를 입력해주세요.");
      return;
    }

    try {
      const token = localStorage.getItem("access_token");
      let endpoint = "";
      if (selectedType === "CAP") {
        endpoint = "cap";
      } else if (selectedType === "ACC") {
        endpoint = "acc";
      } else if (selectedType === "VINTAGE") {
        endpoint = "vintage";
      }
      const response = await fetch(
        `${SERVER}/api/${endpoint}/updateStock/${productId}/${size}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          body: JSON.stringify(stockNumber),
        }
      );

      if (response.ok) {
        alert(`"${size}" 사이즈 재고가 업데이트되었습니다.`);
        await fetchProducts();
      } else {
        alert("재고 업데이트에 실패했습니다.");
      }
    } catch (error) {
      console.error("사이즈별 재고 업데이트 실패:", error);
      alert("오류가 발생했습니다.");
    }
  };

  const handleUpdatePrice = async (
    productId: number,
    productName: string,
    currentPrice: number
  ) => {
    const choice = confirm(
      `"${productName}" 가격 변경 방식을 선택하세요.\n\n확인: 세일 효과 주기 (원가 표시 + 할인가)\n취소: 일반 가격 변경`
    );

    if (choice) {
      await handleApplySale(productId, productName, currentPrice);
    } else {
      await handleSimplePriceChange(productId, productName, currentPrice);
    }
  };

  const handleApplySale = async (
    productId: number,
    productName: string,
    currentPrice: number
  ) => {
    const salePriceInput = prompt(
      `"${productName}" 세일 가격을 입력하세요.\n\n현재 가격: ${currentPrice.toLocaleString()}원\n(현재 가격이 원가로 설정되고, 입력한 가격이 세일가가 됩니다)`,
      String(Math.floor(currentPrice * 0.8))
    );
    if (salePriceInput === null) return;

    const salePrice = parseInt(salePriceInput);
    if (isNaN(salePrice) || salePrice < 0) {
      alert("올바른 숫자를 입력해주세요.");
      return;
    }

    if (salePrice >= currentPrice) {
      alert("세일 가격은 현재 가격보다 낮아야 합니다.");
      return;
    }

    const discountRate = Math.round(
      ((currentPrice - salePrice) / currentPrice) * 100
    );

    if (
      !confirm(
        `세일 적용 확인\n\n원가: ${currentPrice.toLocaleString()}원\n세일가: ${salePrice.toLocaleString()}원\n할인율: ${discountRate}%\n\n적용하시겠습니까?`
      )
    ) {
      return;
    }

    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/product/${productId}/price`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(salePrice),
      });

      if (response.ok) {
        alert(
          `세일이 적용되었습니다!\n${currentPrice.toLocaleString()}원 → ${salePrice.toLocaleString()}원 (${discountRate}% 할인)`
        );
        await fetchProducts();
      } else {
        alert("세일 적용에 실패했습니다.");
      }
    } catch (error) {
      console.error("세일 적용 실패:", error);
      alert("오류가 발생했습니다.");
    }
  };

  const handleSimplePriceChange = async (
    productId: number,
    productName: string,
    currentPrice: number
  ) => {
    const newPrice = prompt(
      `"${productName}" 상품의 가격을 변경합니다.\n현재 가격: ${currentPrice.toLocaleString()}원\n\n새 가격을 입력하세요:`,
      String(currentPrice)
    );
    if (newPrice === null) return;

    const priceNumber = parseInt(newPrice);
    if (isNaN(priceNumber) || priceNumber < 0) {
      alert("올바른 숫자를 입력해주세요.");
      return;
    }

    if (priceNumber === currentPrice) {
      alert("기존 가격과 동일합니다.");
      return;
    }

    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/product/${productId}/price`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(priceNumber),
      });

      if (response.ok) {
        alert(
          `가격이 ${currentPrice.toLocaleString()}원 → ${priceNumber.toLocaleString()}원으로 변경되었습니다.`
        );
        await fetchProducts();
      } else {
        alert("가격 변경에 실패했습니다.");
      }
    } catch (error) {
      console.error("가격 변경 실패:", error);
      alert("오류가 발생했습니다.");
    }
  };

  const handleMarkSizeOutOfStock = async (
    productId: number,
    size: string,
    productName: string
  ) => {
    if (!confirm(`"${productName}" 상품의 "${size}" 사이즈를 품절 처리하시겠습니까?`))
      return;

    try {
      const token = localStorage.getItem("access_token");
      let endpoint = "";
      if (selectedType === "CAP") {
        endpoint = "cap";
      } else if (selectedType === "ACC") {
        endpoint = "acc";
      } else if (selectedType === "VINTAGE") {
        endpoint = "vintage";
      }
      const response = await fetch(
        `${SERVER}/api/${endpoint}/updateStock/${productId}/${size}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          body: JSON.stringify(0),
        }
      );

      if (response.ok) {
        alert(`"${size}" 사이즈가 품절 처리되었습니다.`);
        await fetchProducts();
      } else {
        alert("품절 처리에 실패했습니다.");
      }
    } catch (error) {
      console.error("사이즈별 품절 처리 실패:", error);
      alert("오류가 발생했습니다.");
    }
  };



  const handleDeleteProduct = async (
    productId: number,
    productName: string
  ) => {
    if (!confirm(`"${productName}" 상품을 정말 비활성화하시겠습니까?`)) return;

    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(
        `${SERVER}/api/admin/products/${productId}/deactivate`,
        {
          method: "POST",
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        }
      );

      if (response.ok) {
        alert("상품이 비활성화되었습니다.");
        await fetchProducts();
      } else {
        alert("상품 비활성화에 실패했습니다.");
      }
    } catch (error) {
      console.error("상품 비활성화 실패:", error);
      alert("상품 비활성화에 실패했습니다.");
    }
  };

  const handleActivateProduct = async (
    productId: number,
    productName: string
  ) => {
    if (!confirm(`"${productName}" 상품을 활성화하시겠습니까?`)) return;

    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(
        `${SERVER}/api/admin/products/${productId}/activate`,
        {
          method: "POST",
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        }
      );

      if (response.ok) {
        alert("상품이 활성화되었습니다.");
        await fetchProducts();
      } else {
        alert("상품 활성화에 실패했습니다.");
      }
    } catch (error) {
      console.error("상품 활성화 실패:", error);
      alert("상품 활성화에 실패했습니다.");
    }
  };

  const cleanupUploadedImages = async (urls: string[]) => {
    const token = localStorage.getItem("access_token");
    if (!token) return;
    const namesOrUrls = (urls || []).map((u) => (u || "").trim()).filter(Boolean);
    if (namesOrUrls.length === 0) return;

    try {
      await fetch(`${SERVER}/api/image/delete`, {
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

  const handleOpenImageModal = (product: Product, type: "main" | "details") => {
    setEditingProduct(product);
    setModalType(type);
    setNewMainImage(null);
    setNewImages([]);
    setIsImageModalOpen(true);
  };

  const handleUpdateImages = async () => {
    if (!editingProduct) return;

    try {
      const token = localStorage.getItem("access_token");

      if (modalType === "main") {
        // 메인 이미지만 수정
        if (!newMainImage) {
          alert("메인 이미지를 선택해주세요.");
          return;
        }

        const mainFormData = new FormData();
        mainFormData.append("mainImage", newMainImage);
        mainFormData.append("productId", editingProduct.id.toString());

        const uploadRes = await fetch(`${SERVER}/api/product/upload/main`, {
          method: "POST",
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          body: mainFormData,
        });

        if (!uploadRes.ok) {
          alert("메인 이미지 업로드에 실패했습니다.");
          return;
        }

        // 기존 메인 이미지 삭제
        if (editingProduct.mainImageUrl) {
          await cleanupUploadedImages([editingProduct.mainImageUrl]);
        }

        alert("메인 이미지가 수정되었습니다.");
      } else if (modalType === "details") {
        // 상세 이미지만 수정
        if (newImages.length === 0) {
          alert("상품 이미지를 선택해주세요.");
          return;
        }

        const imagesFormData = new FormData();
        newImages.forEach((file) => {
          imagesFormData.append("images", file);
        });
        imagesFormData.append("productId", editingProduct.id.toString());

        const uploadRes = await fetch(`${SERVER}/api/product/upload/images`, {
          method: "POST",
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          body: imagesFormData,
        });

        if (!uploadRes.ok) {
          alert("상품 이미지 업로드에 실패했습니다.");
          return;
        }

        // 기존 상세 이미지 삭제
        if (productImages[editingProduct.id]) {
          await cleanupUploadedImages(productImages[editingProduct.id]);
        }

        alert("상품 이미지가 수정되었습니다.");
      }

      setIsImageModalOpen(false);
      setEditingProduct(null);
      setNewMainImage(null);
      setNewImages([]);
      await fetchProducts();
    } catch (error) {
      console.error("이미지 수정 실패:", error);
      alert("오류가 발생했습니다.");
    }
  };

  return (
    <div className="border p-4 rounded md:col-span-2 font-sans">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-xl font-semibold">상품 관리</h2>
        </div>
        <button
          onClick={onToggle}
          className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
        >
          {isOpen ? "접기" : "펼치기"}
        </button>
      </div>

      {isOpen && (
        <>
          <div className="flex gap-2 mt-4 border-b">
            <button
              onClick={() => setSelectedType("CAP")}
              className={`px-4 py-2 font-medium transition-colors ${
                selectedType === "CAP"
                  ? "border-b-2 border-blue-600 text-blue-600"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              모자 (CAP)
            </button>
            <button
              onClick={() => setSelectedType("ACC")}
              className={`px-4 py-2 font-medium transition-colors ${
                selectedType === "ACC"
                  ? "border-b-2 border-blue-600 text-blue-600"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              액세서리 (ACC)
            </button>
            <button
              onClick={() => setSelectedType("VINTAGE")}
              className={`px-4 py-2 font-medium transition-colors ${
                selectedType === "VINTAGE"
                  ? "border-b-2 border-blue-600 text-blue-600"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              빈티지 (VINTAGE)
            </button>
          </div>

          {selectedType === "VINTAGE" && (
            <div className="flex gap-2 mt-4">
              <button
                onClick={() => setSelectedVintageCategory("ALL")}
                className={`px-3 py-1.5 rounded text-sm ${
                  selectedVintageCategory === "ALL"
                    ? "bg-black text-white"
                    : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                }`}
              >
                전체
              </button>
              <button
                onClick={() => setSelectedVintageCategory("CAP")}
                className={`px-3 py-1.5 rounded text-sm ${
                  selectedVintageCategory === "CAP"
                    ? "bg-black text-white"
                    : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                }`}
              >
                빈티지 모자
              </button>
              <button
                onClick={() => setSelectedVintageCategory("SHIRT")}
                className={`px-3 py-1.5 rounded text-sm ${
                  selectedVintageCategory === "SHIRT"
                    ? "bg-black text-white"
                    : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                }`}
              >
                빈티지 티셔츠
              </button>
            </div>
          )}

          <div className="flex gap-2 mt-4 border-b">
            <button
              onClick={() => setStatusTab("active")}
              className={`px-4 py-2 font-medium transition-colors ${
                statusTab === "active"
                  ? "border-b-2 border-green-600 text-green-600"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              활성 상품 ({products.filter((p) => p.status !== "INACTIVE").length})
            </button>
            <button
              onClick={() => setStatusTab("inactive")}
              className={`px-4 py-2 font-medium transition-colors ${
                statusTab === "inactive"
                  ? "border-b-2 border-red-600 text-red-600"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              비활성 상품 ({products.filter((p) => p.status === "INACTIVE").length})
            </button>
          </div>

          <div className="flex items-center justify-end gap-2 mt-4">
            <button
              onClick={() => navigate("/register")}
              className="px-3 py-1 bg-blue-600 text-white text-sm rounded bg-blue-300 hover:bg-blue-200"
            >
              상품 등록
            </button>
            <button
              onClick={fetchProducts}
              className="px-3 py-1 border rounded text-sm bg-blue-300 hover:bg-blue-200"
            >
              새로고침
            </button>
          </div>

          <div className="mt-4">
            {products.length === 0 ? (
              <p className="text-gray-600">상품이 없습니다.</p>
            ) : (
              <div className="space-y-3">
                {visibleProducts.map((product) => {
                    const images = [
                      product.mainImageUrl,
                      ...(productImages[product.id] ?? []),
                    ].filter(Boolean);

                    return (
                      <div key={product.id} className="border rounded p-4">
                        <div className="flex items-start gap-4">
                          <div className="flex gap-2 overflow-x-auto pb-2 min-w-0 flex-shrink-0" style={{ maxWidth: '300px' }}>
                            {images.map((img, idx) => {
                              const imageSrc = normalizeImageUrl(img);

                              return (
                                <img
                                  key={`${product.id}-${idx}`}
                                  src={imageSrc}
                                  alt={`${product.name} 이미지 ${idx + 1}`}
                                  className="w-20 h-20 object-cover rounded border flex-shrink-0 cursor-pointer hover:opacity-80 transition-opacity"
                                  onError={() =>
                                    console.log("이미지 로드 실패:", imageSrc)
                                  }
                                />
                              );
                            })}
                          </div>

                          <div className="flex-1">
                            <div className="flex items-start justify-between mb-2">
                              <div>
                                <div className="flex items-center gap-2 flex-wrap">
                                  <h3 className="font-semibold text-lg">
                                    {product.name}
                                  </h3>
                                  {selectedType === "VINTAGE" && (
                                    <span className="px-2 py-0.5 rounded-full text-xs bg-stone-200 text-stone-800">
                                      {getVintageCategoryLabel(
                                        product.vintageCategory
                                      )}
                                    </span>
                                  )}
                                </div>
                                <p className="text-sm text-black">
                                  색상: {product.color}
                                </p>

                                <div className="flex items-center gap-2">
                                  <p className="text-sm text-black">
                                    가격: {product.price.toLocaleString()}원
                                  </p>
                                  <button
                                    onClick={() =>
                                      handleUpdatePrice(
                                        product.id,
                                        product.name,
                                        product.price
                                      )
                                    }
                                    className="px-2 py-1 bg-green-500 text-white text-xs rounded hover:bg-green-600"
                                    title="가격 변경"
                                  >
                                    가격 변경
                                  </button>
                                </div>

                                <div className="flex items-center gap-2">
                                  <p className="text-sm text-black">
                                    재고: {product.stock ?? 0}개
                                  </p>
                                  {(product.stock ?? 0) === 0 && (
                                    <span className="px-2 py-1 bg-red-100 text-red-800 text-xs rounded-full">
                                      품절
                                    </span>
                                  )}
                                  {(product.stock ?? 0) > 0 &&
                                    (product.stock ?? 0) <= 5 && (
                                      <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs rounded-full">
                                        재고 부족
                                      </span>
                                    )}
                                </div>

                                {product.stocks && product.stocks.length > 0 && (
                                  <div className="mt-2">
                                    <p className="text-sm font-medium text-black mb-1">
                                      사이즈별 재고:
                                    </p>
                                    <div className="flex flex-wrap gap-2">
                                      {product.stocks.map((sizeStock) => (
                                        <div
                                          key={sizeStock.size}
                                          className="flex items-center gap-1 px-2 py-1 rounded text-xs"
                                        >
                                          <span className="font-medium">
                                            {sizeStock.size}:
                                          </span>
                                          <span
                                            className={
                                              sizeStock.stock === 0
                                                ? "text-red-800 font-medium"
                                                : ""
                                            }
                                          >
                                            {sizeStock.stock}개
                                          </span>
                                          <button
                                            onClick={() =>
                                              handleUpdateSizeStock(
                                                product.id,
                                                sizeStock.size,
                                                sizeStock.stock
                                              )
                                            }
                                            className="ml-1 px-1 py-0.5 bg-blue-500 text-white rounded hover:bg-blue-600 text-xs"
                                            title="재고 수정"
                                          >
                                            수정
                                          </button>
                                          <button
                                            onClick={() =>
                                              handleMarkSizeOutOfStock(
                                                product.id,
                                                sizeStock.size,
                                                product.name
                                              )
                                            }
                                            className="ml-1 px-1 py-0.5 bg-red-500 text-white rounded hover:bg-red-600 text-xs"
                                            title="품절 처리"
                                            disabled={sizeStock.stock === 0}
                                          >
                                            품절
                                          </button>
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}
                              </div>
                            </div>

                            <div className="flex gap-2 justify-end">
                              <button
                                onClick={() => handleOpenImageModal(product, "main")}
                                className="px-3 py-1.5 bg-blue-600 text-white text-sm rounded hover:bg-blue-700"
                              >
                                메인 이미지 수정
                              </button>
                              <button
                                onClick={() => handleOpenImageModal(product, "details")}
                                className="px-3 py-1.5 bg-indigo-600 text-white text-sm rounded hover:bg-indigo-700"
                              >
                                상품 이미지 수정
                              </button>
                           

                              {product.status === "INACTIVE" ? (
                                <button
                                  onClick={() =>
                                    handleActivateProduct(
                                      product.id,
                                      product.name
                                    )
                                  }
                                  className="px-3 py-1.5 bg-green-600 text-white text-sm rounded hover:bg-green-700"
                                >
                                  활성화
                                </button>
                              ) : (
                                <button
                                  onClick={() =>
                                    handleDeleteProduct(
                                      product.id,
                                      product.name
                                    )
                                  }
                                  className="px-3 py-1.5 bg-red-600 text-white text-sm rounded hover:bg-red-700"
                                >
                                  비활성화
                                </button>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
              </div>
            )}
          </div>
        </>
      )}

      {/* 이미지 수정 모달 */}
      {isImageModalOpen && editingProduct && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-semibold">
                {modalType === "main"
                  ? "메인 이미지 수정"
                  : "상품 이미지 수정"}{" "}
                - {editingProduct.name}
              </h3>
              <button
                onClick={() => {
                  setIsImageModalOpen(false);
                  setEditingProduct(null);
                  setNewMainImage(null);
                  setNewImages([]);
                }}
                className="text-gray-500 hover:text-gray-700 text-2xl"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4">
              {/* 메인 이미지 수정 */}
              {modalType === "main" && (
                <>
                  <div>
                    <p className="text-sm font-medium text-gray-700 mb-2">
                      현재 메인 이미지
                    </p>
                    <img
                      src={normalizeImageUrl(editingProduct.mainImageUrl)}
                      alt="메인 이미지"
                      className="w-32 h-32 object-cover rounded border"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      새 메인 이미지 선택 *
                    </label>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={(e) =>
                        setNewMainImage(e.target.files?.[0] ?? null)
                      }
                      className="w-full border rounded px-3 py-2"
                      required
                    />
                    {newMainImage && (
                      <p className="text-sm text-green-600 mt-1">
                        ✓ {newMainImage.name} 선택됨
                      </p>
                    )}
                  </div>
                </>
              )}

              {/* 상품 이미지 수정 */}
              {modalType === "details" && (
                <>
                  {productImages[editingProduct.id]?.length > 0 && (
                    <div>
                      <p className="text-sm font-medium text-gray-700 mb-2">
                        현재 상품 이미지들
                      </p>
                      <div className="flex gap-2 overflow-x-auto pb-2">
                        {productImages[editingProduct.id].map((img, idx) => (
                          <img
                            key={idx}
                            src={normalizeImageUrl(img)}
                            alt={`상품 이미지 ${idx + 1}`}
                            className="w-20 h-20 object-cover rounded border flex-shrink-0"
                          />
                        ))}
                      </div>
                    </div>
                  )}

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      새 상품 이미지 선택 (여러 장) *
                    </label>
                    <input
                      type="file"
                      accept="image/*"
                      multiple
                      onChange={(e) =>
                        setNewImages(
                          e.target.files ? Array.from(e.target.files) : []
                        )
                      }
                      className="w-full border rounded px-3 py-2"
                      required
                    />
                    {newImages.length > 0 && (
                      <p className="text-sm text-green-600 mt-1">
                        ✓ {newImages.length}개 이미지 선택됨
                      </p>
                    )}
                  </div>
                </>
              )}

              {/* 버튼들 */}
              <div className="flex gap-2 justify-end mt-6">
                <button
                  onClick={() => {
                    setIsImageModalOpen(false);
                    setEditingProduct(null);
                    setNewMainImage(null);
                    setNewImages([]);
                  }}
                  className="px-4 py-2 border rounded text-gray-700 hover:bg-gray-100"
                >
                  취소
                </button>
                <button
                  onClick={handleUpdateImages}
                  className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
                >
                  수정하기
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
