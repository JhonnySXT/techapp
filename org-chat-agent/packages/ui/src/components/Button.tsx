import { forwardRef } from "react";
import type { ButtonHTMLAttributes } from "react";
import clsx from "clsx";

const baseStyles =
  "inline-flex items-center justify-center rounded-md font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 disabled:opacity-60 disabled:cursor-not-allowed";

const variants = {
  primary: "bg-indigo-600 text-white hover:bg-indigo-500 focus-visible:outline-indigo-600",
  secondary:
    "bg-slate-800 text-white hover:bg-slate-700 focus-visible:outline-slate-800 dark:bg-slate-100 dark:text-slate-900 dark:hover:bg-white",
  ghost: "bg-transparent text-slate-900 hover:bg-slate-100 dark:text-white dark:hover:bg-slate-800",
};

export type ButtonVariant = keyof typeof variants;

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "primary", type = "button", ...props }, ref) => (
    <button
      ref={ref}
      type={type}
      className={clsx(baseStyles, variants[variant], className)}
      {...props}
    />
  ),
);

Button.displayName = "Button";

