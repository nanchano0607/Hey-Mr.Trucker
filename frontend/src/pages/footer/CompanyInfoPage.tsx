export default function CompanyInfoPage() {
  return (
    <div className="font-sans min-h-screen bg-gray-500 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto bg-gray-200 rounded-lg shadow-md p-8 mt-16">
        <h1 className="text-3xl font-bold mb-8">사업자정보</h1>

        <div className="prose prose-sm max-w-none">
          <div className="bg-gray-100 rounded-lg p-6 space-y-4">
            <div>
              <h3 className="font-semibold text-gray-600 mb-1">상호명</h3>
              <p className="text-lg text-gray-900">미스터컴퍼니</p>
            </div>

            <div>
              <h3 className="font-semibold text-gray-600 mb-1">대표자</h3>
              <p className="text-lg text-gray-900">김건호</p>
            </div>

            <div>
              <h3 className="font-semibold text-gray-600 mb-1">사업자등록번호</h3>
              <p className="text-lg text-gray-900">110-53-02000</p>
            </div>

            <div>
              <h3 className="font-semibold text-gray-600 mb-1">통신판매업 신고번호</h3>
              <p className="text-lg text-gray-900">추후 기재 예정</p>
            </div>

            <div>
              <h3 className="font-semibold text-gray-600 mb-1">주소</h3>
              <p className="text-lg text-gray-900">경기도 수원시 팔달구 효원로12번길 11-15, 1층 101호(고등동)</p>
            </div>

            <div>
              <h3 className="font-semibold text-gray-600 mb-1">유선 번호</h3>
              <p className="text-lg text-gray-900">070-8098-2611</p>
            </div>

            <div>
              <h3 className="font-semibold text-gray-600 mb-1">이메일</h3>
              <p className="text-lg text-gray-900">
                <a href="mailto:gss0227@naver.com" className="text-blue-600 hover:underline">
                  gss0227@naver.com
                </a>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
