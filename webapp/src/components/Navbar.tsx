import * as React from "react";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import IconButton from "@mui/material/IconButton";
import Badge from "@mui/material/Badge";
import MenuItem from "@mui/material/MenuItem";
import Menu from "@mui/material/Menu";
import MenuIcon from "@mui/icons-material/Menu";
import AccountCircle from "@mui/icons-material/AccountCircle";
import MailIcon from "@mui/icons-material/Mail";
import NotificationsIcon from "@mui/icons-material/Notifications";
import MoreIcon from "@mui/icons-material/MoreVert";
import { Button } from "@mui/material";
import LoginDialog from "./LoginDialog";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import Avatar from "@mui/material/Avatar";
import AddIcon from "@mui/icons-material/Add";
import SubscriptionForm from "./SubscriptionForm";
import { useDispatch, useSelector } from "react-redux";
import Subtrak from "../../public/Subtrak.png";
import { useNavigate } from "react-router-dom";

export default function Navbar({
  onClick,
  open,
  showDrawerButton = false,
}: {
  onClick: () => void;
  open: boolean;
  showDrawerButton?: boolean;
}) {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [mobileMoreAnchorEl, setMobileMoreAnchorEl] = React.useState<null | HTMLElement>(null);
  const [openLogin, setOpenLogin] = React.useState(false);
  const [openAdd, setOpenAdd] = React.useState(false);
  const user = useSelector((state: any) => state.auth.user);
  const dispatch = useDispatch();
  const isMenuOpen = Boolean(anchorEl);
  const isMobileMenuOpen = Boolean(mobileMoreAnchorEl);
  const navigate = useNavigate();

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
      <MenuItem>
        <IconButton size="large" aria-label="show 4 new mails" color="inherit">
          <Badge badgeContent={4} color="error">
            <MailIcon />
          </Badge>
        </IconButton>
        <p>Messages</p>
      </MenuItem>
      <MenuItem>
        <IconButton size="large" aria-label="show 17 new notifications" color="inherit">
          <Badge badgeContent={17} color="error">
            <NotificationsIcon />
          </Badge>
        </IconButton>
        <p>Notifications</p>
      </MenuItem>
      <MenuItem onClick={handleProfileMenuOpen}>
        <IconButton
          size="large"
          aria-label="account of current user"
          aria-controls="primary-search-account-menu"
          aria-haspopup="true"
          color="inherit"
        >
          <AccountCircle />
        </IconButton>
        <p>Profile</p>
      </MenuItem>
    </Menu>
  );

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar
        position="fixed"
        sx={{
          zIndex: (theme) => theme.zIndex.drawer + 1,
          transition: "margin 0.3s, width 0.3s",
          ml: open ? "240px" : "64px",
          width: open ? "calc(100% - 240px)" : "100%",
        }}
      >
        <Toolbar>
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
              src={Subtrak}
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
                <IconButton onClick={handleProfileMenuOpen}>
                  <Avatar
                    src={user.picture}
                    alt={user?.name}
                    sx={{ width: 36, height: 36 }}
                    imgProps={{ referrerPolicy: "no-referrer" }}
                  />
                </IconButton>
              </>
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
                  background: "#c468dd",
                }}
              >
                Login
              </Button>
            )}
          </Box>
          <Box sx={{ display: { xs: "flex", md: "none" } }}>
            <IconButton
              size="large"
              aria-label="show more"
              aria-controls={mobileMenuId}
              aria-haspopup="true"
              onClick={handleMobileMenuOpen}
              color="inherit"
            >
              <MoreIcon />
            </IconButton>
            <LoginDialog open={openLogin} onClose={() => setOpenLogin(false)} />
          </Box>
        </Toolbar>
      </AppBar>
      {renderMobileMenu}
      {renderMenu}
      <SubscriptionForm
        open={openAdd}
        handleClose={() => setOpenAdd(false)}
        onSuccess={() => window.dispatchEvent(new Event("subscription_added"))}
      />
    </Box>
  );
}
