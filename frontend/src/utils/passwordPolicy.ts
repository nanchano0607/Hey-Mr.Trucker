/**
 * 새 비밀번호(회원가입, 재설정) 정책. 서버의 PasswordPolicy 와 같은 규칙을 화면에서 미리 안내한다.
 * 영문·숫자·특수문자를 각각 1개 이상 포함한 8자 이상, 공백 불가, UTF-8 72바이트 이하.
 * 로그인에는 적용하지 않는다. (정책 도입 이전에 가입한 회원의 비밀번호도 그대로 유효)
 */
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_BYTES = 72;
export const PASSWORD_GUIDANCE = `비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해 ${PASSWORD_MIN_LENGTH}자 이상이어야 합니다.`;

/** 정책에 맞으면 null, 아니면 사용자에게 보여줄 사유를 돌려준다. */
export function validatePassword(password: string): string | null {
  if (!password || password.trim().length === 0) {
    return "비밀번호를 입력해주세요.";
  }
  if (/\s/.test(password)) {
    return "비밀번호에는 공백을 사용할 수 없습니다.";
  }
  if (new TextEncoder().encode(password).length > PASSWORD_MAX_BYTES) {
    return `비밀번호가 너무 깁니다. (최대 ${PASSWORD_MAX_BYTES}바이트: 영문·숫자 기준 ${PASSWORD_MAX_BYTES}자, 한글 기준 24자)`;
  }

  const characters = Array.from(password);
  const hasLetter = /[A-Za-z]/.test(password);
  const hasDigit = /[0-9]/.test(password);
  // 문자·숫자·공백이 아닌 문자. 한글은 문자이므로 특수문자로 치지 않는다.
  const hasSpecial = characters.some((ch) => !/[\p{L}\p{Nd}\s]/u.test(ch));

  if (characters.length < PASSWORD_MIN_LENGTH || !hasLetter || !hasDigit || !hasSpecial) {
    return PASSWORD_GUIDANCE;
  }
  return null;
}
