import { useEffect } from "react";

type LightboxProps = {
  imageSrc: string;
  isOpen: boolean;
  onClose: () => void;
};

export default function Lightbox({ imageSrc, isOpen, onClose }: LightboxProps) {
  // ESC 키로 닫기
  useEffect(() => {
    const handleEscKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && isOpen) {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleEscKey);
      // 배경 스크롤 방지
      document.body.style.overflow = 'hidden';
    }

    return () => {
      document.removeEventListener('keydown', handleEscKey);
      document.body.style.overflow = 'unset';
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-transparent flex items-center justify-center p-4">
      {/* 닫기 버튼 */}
      <button
        onClick={onClose}
        className="absolute top-4 right-4 text-black hover:text-gray-600 text-3xl font-bold z-10 transition-colors bg-white bg-opacity-80 rounded-full w-10 h-10 flex items-center justify-center"
      >
        ✕
      </button>
      
      {/* 확대된 이미지 */}
      <img
        src={imageSrc}
        alt="확대된 이미지"
        className="max-w-full max-h-full object-contain cursor-pointer shadow-2xl"
        onClick={onClose}
      />
      
      {/* 배경 클릭으로 닫기 */}
      <div 
        className="absolute inset-0 -z-10"
        onClick={onClose}
      />
    </div>
  );
}