import { configureStore } from "@reduxjs/toolkit";
import { render, screen } from "@testing-library/react";
import { axe } from "jest-axe";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import authReducer, { logout, setAuth } from "../app/authSlice";
import ProtectedRoute from "./ProtectedRoutes";

const renderProtectedRoute = (
  state: "loading" | "anonymous" | "authenticated",
) => {
  const store = configureStore({ reducer: { auth: authReducer } });

  if (state === "anonymous") {
    store.dispatch(logout());
  }
  if (state === "authenticated") {
    store.dispatch(
      setAuth({
        token: "access-token",
        user: { name: "Test User", email: "user@example.com", picture: "" },
      }),
    );
  }

  return render(
    <Provider store={store}>
      <MemoryRouter initialEntries={["/private"]}>
        <Routes>
          <Route path="/" element={<h1>Public home</h1>} />
          <Route
            path="/private"
            element={
              <ProtectedRoute>
                <h1>Private dashboard</h1>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );
};

describe("ProtectedRoute", () => {
  it("does not expose protected content while session access is unresolved", () => {
    const { container } = renderProtectedRoute("loading");

    expect(screen.queryByRole("heading", { name: "Private dashboard" })).not.toBeInTheDocument();
    expect(screen.getByRole("progressbar")).toBeInTheDocument();
    expect(container).not.toHaveTextContent("Private dashboard");
  });

  it("redirects anonymous users away from protected content", () => {
    renderProtectedRoute("anonymous");

    expect(screen.getByRole("heading", { name: "Public home" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Private dashboard" })).not.toBeInTheDocument();
  });

  it("grants authenticated users access to protected content", async () => {
    const { container } = renderProtectedRoute("authenticated");

    expect(screen.getByRole("heading", { name: "Private dashboard" })).toBeInTheDocument();
    expect(await axe(container)).toHaveNoViolations();
  });
});
