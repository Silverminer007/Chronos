import type {Config} from "tailwindcss";
import withMT from "@material-tailwind/react/utils/withMT";

export default withMT({
    content: [
        "./app/**/{**,.client,.server}/**/*.{js,jsx,ts,tsx}",
        "node_modules/@material-tailwind/react/components/**/*.{js,ts,jsx,tsx}",
        "node_modules/@material-tailwind/react/theme/components/**/*.{js,ts,jsx,tsx}"
    ],
    theme: {
        extend: {
            fontFamily: {
                sans: [
                    '"Inter"',
                    "ui-sans-serif",
                    "system-ui",
                    "sans-serif",
                    '"Apple Color Emoji"',
                    '"Segoe UI Emoji"',
                    '"Segoe UI Symbol"',
                    '"Noto Color Emoji"',
                ],
            },
        },
    },
    plugins: [],
} satisfies Config);
