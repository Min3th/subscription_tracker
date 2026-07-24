import * as React from "react";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import IconButton from "@mui/material/IconButton";
import MenuItem from "@mui/material/MenuItem";
import Menu from "@mui/material/Menu";
import MoreIcon from "@mui/icons-material/MoreVert";
import MenuIcon from "@mui/icons-material/Menu";
import LogoutIcon from "@mui/icons-material/Logout";
import { Button } from "@mui/material";
import LoginDialog from "./LoginDialog";
import Avatar from "@mui/material/Avatar";
import AddIcon from "@mui/icons-material/Add";
import SubscriptionForm from "./SubscriptionForm";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate, useLocation } from "react-router-dom";
import type { AppDispatch, RootState } from "../app/store";
import { logoutUser } from "../app/authSlice";

export default function Navbar({
  onClick,
  open,
  showDrawerButton = false,
}: {
  onClick?: () => void;
  open: boolean;
  showDrawerButton?: boolean;
}) {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [mobileMoreAnchorEl, setMobileMoreAnchorEl] = React.useState<null | HTMLElement>(null);
  const [openLogin, setOpenLogin] = React.useState(false);
  const [openAdd, setOpenAdd] = React.useState(false);
  const user = useSelector((state: RootState) => state.auth.user);
  const dispatch = useDispatch<AppDispatch>();
  const isMenuOpen = Boolean(anchorEl);
  const isMobileMenuOpen = Boolean(mobileMoreAnchorEl);
  const navigate = useNavigate();
  const location = useLocation();

  // check if landing page
  const isLandingPage = location.pathname === "/";

  React.useEffect(() => {
    const handleOpenAdd = () => setOpenAdd(true);
    window.addEventListener("open_add_subscription", handleOpenAdd);
    return () => window.removeEventListener("open_add_subscription", handleOpenAdd);
  }, []);

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMobileMenuClose = () => {
    setMobileMoreAnchorEl(null);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    handleMobileMenuClose();
  };

  const handleMobileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setMobileMoreAnchorEl(event.currentTarget);
  };

  const handleMobileSectionNavigate = (sectionId: string) => {
    handleMobileMenuClose();
    document.getElementById(sectionId)?.scrollIntoView({ behavior: "smooth" });
  };

  const handleLogout = async () => {
    handleMenuClose();
    await dispatch(logoutUser());
    navigate("/", { replace: true });
  };

  const menuId = "primary-search-account-menu";
  const renderMenu = (
    <Menu
      anchorEl={anchorEl}
      anchorOrigin={{
        vertical: "top",
        horizontal: "right",
      }}
      id={menuId}
      keepMounted
      transformOrigin={{
        vertical: "top",
        horizontal: "right",
      }}
      open={isMenuOpen}
      onClose={handleMenuClose}
    >
      <MenuItem onClick={handleMenuClose}>Profile</MenuItem>
      <MenuItem onClick={handleMenuClose}>My account</MenuItem>
      <MenuItem onClick={handleLogout} sx={{ color: "error.main", gap: 1 }}>
        <LogoutIcon fontSize="small" />
        Logout
      </MenuItem>
    </Menu>
  );

  const mobileMenuId = "primary-search-account-menu-mobile";
  const renderMobileMenu = (
    <Menu
      anchorEl={mobileMoreAnchorEl}
      anchorOrigin={{
        vertical: "top",
        horizontal: "right",
      }}
      id={mobileMenuId}
      keepMounted
      transformOrigin={{
        vertical: "top",
        horizontal: "right",
      }}
      open={isMobileMenuOpen}
      onClose={handleMobileMenuClose}
    >
      {!showDrawerButton && [
        ["why-you-need", "Why Track"],
        ["features", "Features"],
        ["how-it-works", "How It Works"],
      ].map(([sectionId, label]) => (
        <MenuItem
          key={sectionId}
          onClick={() => handleMobileSectionNavigate(sectionId)}
          sx={{ minHeight: 48 }}
        >
          {label}
        </MenuItem>
      ))}
      {user ? [
        !isLandingPage ? (
          <MenuItem
            key="add-subscription"
            onClick={() => {
              handleMobileMenuClose();
              setOpenAdd(true);
            }}
            sx={{ minHeight: 48, gap: 1 }}
          >
            <AddIcon fontSize="small" />
            Add Subscription
          </MenuItem>
        ) : null,
        <MenuItem
          key="account-settings"
          onClick={() => {
            handleMobileMenuClose();
            navigate("/settings");
          }}
          sx={{ minHeight: 48 }}
        >
          Account Settings
        </MenuItem>,
        <MenuItem
          key="logout"
          onClick={handleLogout}
          sx={{ minHeight: 48, color: "error.main", gap: 1 }}
        >
          <LogoutIcon fontSize="small" />
          Logout
        </MenuItem>,
      ] : (
        <MenuItem
          onClick={() => {
            handleMobileMenuClose();
            setOpenLogin(true);
          }}
          sx={{ minHeight: 48 }}
        >
          Login
        </MenuItem>
      )}
    </Menu>
  );

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar
        position="fixed"
        sx={{
          zIndex: (theme) => ({
            xs: theme.zIndex.appBar,
            md: theme.zIndex.drawer + 1,
          }),
          transition: "margin 0.3s, width 0.3s",
          ml: { xs: 0, md: open ? "240px" : "64px" },
          width: { xs: "100%", md: open ? "calc(100% - 240px)" : "100%" },
        }}
      >
        <Toolbar>
          {showDrawerButton && (
            <IconButton
              color="inherit"
              aria-label="Open navigation"
              edge="start"
              onClick={onClick}
              sx={{ display: { xs: "inline-flex", md: "none" }, mr: 1 }}
            >
              <MenuIcon />
            </IconButton>
          )}
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 1,
              cursor: "pointer",
            }}
            onClick={() => navigate("/")}
          >
            <Box
              component="img"
              src="/Subtrak.png"
              alt="Subtrak Logo"
              sx={{
                height: 40,
                width: "auto",
              }}
            />
          </Box>
          {!showDrawerButton && (
            <Box
              sx={{
                display: { xs: "none", md: "flex" },
                gap: 3,
                alignItems: "center",
                position: "absolute",
                left: "50%",
                transform: "translateX(-50%)",
              }}
            >
              <Button
                color="inherit"
                onClick={() => document.getElementById("why-you-need")?.scrollIntoView({ behavior: "smooth" })}
                sx={{ textTransform: "none", fontWeight: 500 }}
              >
                Why Track
              </Button>
              <Button
                color="inherit"
                onClick={() => document.getElementById("features")?.scrollIntoView({ behavior: "smooth" })}
                sx={{ textTransform: "none", fontWeight: 500 }}
              >
                Features
              </Button>
              <Button
                color="inherit"
                onClick={() => document.getElementById("how-it-works")?.scrollIntoView({ behavior: "smooth" })}
                sx={{ textTransform: "none", fontWeight: 500 }}
              >
                How It Works
              </Button>
            </Box>
          )}

          <Box sx={{ flexGrow: 1 }} />
          <Box sx={{ display: { xs: "none", md: "flex" } }}>
            {user ? (
              !isLandingPage && (
                <>
                  <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    sx={{
                      mr: 2,
                      my: "auto",
                      px: 2,
                      py: 1,
                      height: "fit-content",
                      borderRadius: "999px",
                      textTransform: "none",
                      fontWeight: 600,
                      fontSize: "0.9rem",
                      boxShadow: "0px 4px 10px rgba(0, 0, 0, 0.15)",
                      "&:hover": {
                        boxShadow: "0px 6px 14px rgba(0, 0, 0, 0.25)",
                      },
                    }}
                    onClick={() => setOpenAdd(true)}
                  >
                    Add Subscription
                  </Button>
                </>
              )
            ) : (
              <Button
                variant="contained"
                onClick={() => setOpenLogin(true)}
                sx={{
                  ml: 2,
                  px: 3,
                  py: 1,
                  borderRadius: "999px",
                  textTransform: "none",
                  fontWeight: 600,
                  fontSize: "0.9rem",
                  background: "#6400ef",
                  "&:hover": {
                    background: "#5000c0",
                  },
                }}
              >
                Login
              </Button>
            )}
            {user && (
              <IconButton aria-label="Open account menu" onClick={handleProfileMenuOpen}>
                <Avatar
                  src={user.picture}
                  alt={user?.name}
                  sx={{ width: 36, height: 36 }}
                  imgProps={{ referrerPolicy: "no-referrer" }}
                />
              </IconButton>
            )}
          </Box>
          <Box sx={{ display: { xs: "flex", md: "none" } }}>
            <IconButton
              size="large"
              aria-label="Open navigation menu"
              aria-controls={mobileMenuId}
              aria-haspopup="true"
              onClick={handleMobileMenuOpen}
              color="inherit"
            >
              <MoreIcon />
            </IconButton>
          </Box>
        </Toolbar>
      </AppBar>
      {renderMobileMenu}
      {renderMenu}
      <LoginDialog open={openLogin} onClose={() => setOpenLogin(false)} />
      <SubscriptionForm
        open={openAdd}
        handleClose={() => setOpenAdd(false)}
        onSuccess={() => window.dispatchEvent(new Event("subscription_added"))}
      />
    </Box>
  );
}
