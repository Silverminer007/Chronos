import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
    index("routes/home.tsx"),
    route("dates", "routes/dates.tsx"),
    route("calendar", "routes/calendar.tsx")
] satisfies RouteConfig;
