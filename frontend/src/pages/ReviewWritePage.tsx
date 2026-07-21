import { useState, useEffect, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { API_BASE_URL } from "../config/apiBase";

const SERVER = API_BASE_URL;

type ReviewCreateRequest = {
  userId: number;
  productId: number;
  orderId?: number; // 주문 검증용
  rating: number; // 1-5
  content: string;
  imageUrls: string[];
};

export default function ReviewWritePage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  
  // URL 파라미터에서 productId, orderId 추출
  const productId = searchParams.get("productId");
  const orderId = searchParams.get("orderId");
  
  const [rating, setRating] = useState(5);
  const [content, setContent] = useState("");
  const [images, setImages] = useState<File[]>([]);
  const [previewUrls, setPreviewUrls] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [uploading, setUploading] = useState(false);
  
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    // 로그인 확인
    if (!user) {
      alert("로그인이 필요합니다.");
      navigate("/login");
      return;
    }
    
    // productId 필수 체크
    if (!productId) {
      alert("상품 정보가 없습니다.");
      navigate(-1);
      return;
    }
  }, [user, productId, navigate]);

  // 이미지 선택
  const handleImageSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files || []);
    if (files.length === 0) return;
    
    // 최대 5개 제한
    const totalImages = images.length + files.length;
    if (totalImages > 5) {
      alert("이미지는 최대 5개까지 업로드할 수 있습니다.");
      return;
    }
    
    // 미리보기 URL 생성
    const newPreviewUrls = files.map(file => URL.createObjectURL(file));
    
    setImages(prev => [...prev, ...files]);
    setPreviewUrls(prev => [...prev, ...newPreviewUrls]);
  };

  // 이미지 삭제
  const removeImage = (index: number) => {
    // 미리보기 URL 해제
    URL.revokeObjectURL(previewUrls[index]);
    
    setImages(prev => prev.filter((_, i) => i !== index));
    setPreviewUrls(prev => prev.filter((_, i) => i !== index));
  };

  // 이미지 업로드
  const uploadImages = async (): Promise<string[]> => {
    if (images.length === 0) return [];
    
    setUploading(true);
    try {
      const formData = new FormData();
      images.forEach(image => {
        formData.append("images", image);
      });
      
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/review/upload`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      });
      
      if (!response.ok) {
        throw new Error("이미지 업로드에 실패했습니다.");
      }
      
      const data = await response.json();
      return data.imageUrls || [];
    } catch (error) {
      console.error("이미지 업로드 실패:", error);
      throw error;
    } finally {
      setUploading(false);
    }
  };

  // 리뷰 제출
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!user || !productId) return;
    
    if (content.trim().length < 7) {
      alert("리뷰 내용은 최소 7자 이상 작성해주세요.");
      return;
    }
    
    setSubmitting(true);
    try {
      // 1. 이미지 업로드
      const uploadedImageUrls = await uploadImages();
      
      // 2. 리뷰 생성 요청
      const reviewData: ReviewCreateRequest = {
        userId: user.id!,
        productId: parseInt(productId),
        orderId: orderId ? parseInt(orderId) : undefined,
        rating,
        content: content.trim(),
        imageUrls: uploadedImageUrls,
      };
      
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${SERVER}/api/reviews`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(reviewData),
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || "리뷰 등록에 실패했습니다.");
      }

      alert("리뷰가 성공적으로 등록되었습니다!");
      
      // 3. 주문 페이지로 이동 (orderId가 있으면 주문 페이지, 없으면 상품 페이지)
      if (orderId) {
        navigate("/order"); // 주문 페이지로 이동
      } else {
        navigate(`/cap/${productId}`); // 상품 상세 페이지로 이동
      }
      
    } catch (error: any) {
      console.error("리뷰 제출 실패:", error);
      alert(error.message || "리뷰 등록 중 오류가 발생했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  // 별점 렌더링
  const renderStars = () => {
    return (
      <div className="flex gap-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            onClick={() => setRating(star)}
            className={`text-3xl ${
              star <= rating ? "text-yellow-400" : "text-gray-300"
            } hover:text-yellow-400 transition-colors`}
          >
            ★
          </button>
        ))}
      </div>
    );
  };

  if (!user || !productId) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-100 py-8">
      <div className="max-w-2xl mx-auto px-4">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h1 className="text-2xl font-bold mb-6">리뷰 작성</h1>
          
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* 별점 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                별점
              </label>
              {renderStars()}
              <p className="text-sm text-gray-500 mt-1">
                현재 평점: {rating}점
              </p>
            </div>
            
            {/* 리뷰 내용 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                리뷰 내용 *
              </label>
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={6}
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="상품에 대한 솔직한 후기를 작성해주세요 (최소 10자 이상)"
                required
              />
              <p className="text-sm text-gray-500 mt-1">
                {content.length}/500자
              </p>
            </div>
            
            {/* 이미지 업로드 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                사진 첨부 (선택, 최대 5장)
              </label>
              
              <input
                ref={fileInputRef}
                type="file"
                multiple
                accept="image/*"
                onChange={handleImageSelect}
                className="hidden"
              />
              
              {previewUrls.length < 5 && (
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="border-2 border-dashed border-gray-300 rounded-lg p-4 w-full text-center hover:border-gray-400 transition-colors"
                >
                  <span className="text-gray-500">+ 사진 추가</span>
                </button>
              )}
              
              {/* 이미지 미리보기 */}
              {previewUrls.length > 0 && (
                <div className="mt-4 grid grid-cols-3 gap-4">
                  {previewUrls.map((url, index) => (
                    <div key={index} className="relative">
                      <img
                        src={url}
                        alt={`미리보기 ${index + 1}`}
                        className="w-full h-24 object-cover rounded-lg border"
                      />
                      <button
                        type="button"
                        onClick={() => removeImage(index)}
                        className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full w-6 h-6 flex items-center justify-center text-sm hover:bg-red-600"
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
            
            {/* 버튼 */}
            <div className="flex gap-4 pt-4">
              <button
                type="button"
                onClick={() => navigate(-1)}
                className="flex-1 py-2 px-4 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
              >
                취소
              </button>
              <button
                type="submit"
                disabled={submitting || uploading || content.trim().length < 10}
                className="flex-1 py-2 px-4 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? "등록 중..." : uploading ? "업로드 중..." : "리뷰 등록"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
