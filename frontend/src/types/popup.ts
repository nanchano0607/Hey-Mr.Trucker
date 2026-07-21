import { withBase } from "../config/apiBase";

export interface Popup {
  id?: number;
  imageUrl?: string | null;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export function resolvePopupImageUrl(imageUrl?: string | null) {
  const value = imageUrl?.trim();

  if (!value) {
    return "";
  }

  if (
    value.startsWith("http://") ||
    value.startsWith("https://") ||
    value.startsWith("data:")
  ) {
    return value;
  }

  return withBase(value.startsWith("/") ? value : `/${value}`);
}
