import { useState } from "react"
import { type Table } from "@tanstack/react-table"
import { Trash2, UserX, UserCheck, Mail } from "lucide-react"
import { useTranslation } from "react-i18next"
import { toast } from "sonner"
import { sleep } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { DataTableBulkActions as BulkActionsToolbar } from "@/components/data-table"
import { type User } from "../data/schema"
import { UsersMultiDeleteDialog } from "./users-multi-delete-dialog"

type DataTableBulkActionsProps<TData> = {
  table: Table<TData>
}

export function DataTableBulkActions<TData>({
  table,
}: DataTableBulkActionsProps<TData>) {
  const { t } = useTranslation()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const selectedRows = table.getFilteredSelectedRowModel().rows
  const entityLabel =
    selectedRows.length === 1
      ? t("users.entity.singular")
      : t("users.entity.plural")

  const handleBulkStatusChange = (status: "active" | "inactive") => {
    const selectedUsers = selectedRows.map((row) => row.original as User)
    toast.promise(sleep(2000), {
      loading: t(
        status === "active"
          ? "users.bulk.activating"
          : "users.bulk.deactivating"
      ),
      success: () => {
        table.resetRowSelection()
        return t(
          status === "active"
            ? "users.bulk.activated"
            : "users.bulk.deactivated",
          {
            count: selectedUsers.length,
            entity: entityLabel,
          }
        )
      },
      error: t("users.bulk.statusError", {
        action: t(
          status === "active"
            ? "users.bulk.activatingAction"
            : "users.bulk.deactivatingAction"
        ),
      }),
    })
    table.resetRowSelection()
  }

  const handleBulkInvite = () => {
    const selectedUsers = selectedRows.map((row) => row.original as User)
    toast.promise(sleep(2000), {
      loading: t("users.bulk.inviting"),
      success: () => {
        table.resetRowSelection()
        return t("users.bulk.invited", {
          count: selectedUsers.length,
          entity: entityLabel,
        })
      },
      error: t("users.bulk.inviteError"),
    })
    table.resetRowSelection()
  }

  return (
    <>
      <BulkActionsToolbar
        table={table}
        entityName={t("users.entity.singular")}
        entityNamePlural={t("users.entity.plural")}
      >
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="outline"
              size="icon"
              onClick={handleBulkInvite}
              className="size-8"
              aria-label={t("users.bulk.invite")}
              title={t("users.bulk.invite")}
            >
              <Mail />
              <span className="sr-only">{t("users.bulk.invite")}</span>
            </Button>
          </TooltipTrigger>
          <TooltipContent>
            <p>{t("users.bulk.invite")}</p>
          </TooltipContent>
        </Tooltip>

        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="outline"
              size="icon"
              onClick={() => handleBulkStatusChange("active")}
              className="size-8"
              aria-label={t("users.bulk.activate")}
              title={t("users.bulk.activate")}
            >
              <UserCheck />
              <span className="sr-only">{t("users.bulk.activate")}</span>
            </Button>
          </TooltipTrigger>
          <TooltipContent>
            <p>{t("users.bulk.activate")}</p>
          </TooltipContent>
        </Tooltip>

        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="outline"
              size="icon"
              onClick={() => handleBulkStatusChange("inactive")}
              className="size-8"
              aria-label={t("users.bulk.deactivate")}
              title={t("users.bulk.deactivate")}
            >
              <UserX />
              <span className="sr-only">{t("users.bulk.deactivate")}</span>
            </Button>
          </TooltipTrigger>
          <TooltipContent>
            <p>{t("users.bulk.deactivate")}</p>
          </TooltipContent>
        </Tooltip>

        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="destructive"
              size="icon"
              onClick={() => setShowDeleteConfirm(true)}
              className="size-8"
              aria-label={t("users.bulk.delete")}
              title={t("users.bulk.delete")}
            >
              <Trash2 />
              <span className="sr-only">{t("users.bulk.delete")}</span>
            </Button>
          </TooltipTrigger>
          <TooltipContent>
            <p>{t("users.bulk.delete")}</p>
          </TooltipContent>
        </Tooltip>
      </BulkActionsToolbar>

      <UsersMultiDeleteDialog
        table={table}
        open={showDeleteConfirm}
        onOpenChange={setShowDeleteConfirm}
      />
    </>
  )
}
